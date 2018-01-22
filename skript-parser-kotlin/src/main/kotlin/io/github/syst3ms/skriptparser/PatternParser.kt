package io.github.syst3ms.skriptparser

import io.github.syst3ms.skriptparser.classes.PatternType
import io.github.syst3ms.skriptparser.registration.TypeManager
import io.github.syst3ms.skriptparser.pattern.*
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

class PatternParser {

    /**
     * Parses a pattern and returns a [PatternElement]. This method can be called by itself, for example when parsing group constructs.
     * @param pattern the pattern to be parsed
     * @return the parsed PatternElement, or null if something went wrong.
     */
    fun parsePattern(pattern: String): PatternElement? {
        val elements = arrayListOf<PatternElement>()
        var textBuilder = StringBuilder("")
        var i = 0
        while (i < pattern.length) {
            val c = pattern[i]
            when (c) {
                '[' -> {
                    val s = pattern.getEnclosedText('[', ']', i)
                    if (s == null) {
                        error("Unclosed optional group at index $i")
                        return null
                    }
                    if (textBuilder.isNotEmpty()) {
                        elements.add(TextElement(textBuilder.toString()))
                        textBuilder.setLength(0)
                    }
                    i += s.length + 1 // sets i to the closing bracket, for loop does the rest
                    val m = PARSE_MARK_PATTERN.matcher(s)
                    val content: PatternElement
                    content = if (m.matches()) {
                        val mark = m.group(1)
                        val markNumber = mark.toInt()
                        val rest = s.substring(mark.length + 1, s.length)
                        val e = parsePattern(rest) ?: return null
                        ChoiceGroup(listOf(ChoiceElement(e, markNumber))) // I said I would keep the other constructor for unit tests
                    } else {
                        parsePattern(s) ?: return null
                    }
                    elements.add(OptionalGroup(content))
                }
                '(' -> {
                    val s = pattern.getEnclosedText('(', ')', i)
                    if (s == null) {
                        error("Unclosed choice group at index $i")
                        return null
                    }
                    if (textBuilder.isNotEmpty()) {
                        elements.add(TextElement(textBuilder.toString()))
                        textBuilder.setLength(0)
                    }
                    i += s.length + 1
                    val choices = s.split("(?<!\\\\)\\|".toRegex()).dropLastWhile { it.isEmpty() }
                    val choiceElements = arrayListOf<ChoiceElement>()
                    for (choice in choices) {
                        val matcher = PARSE_MARK_PATTERN.matcher(choice)
                        if (matcher.matches()) {
                            val mark = matcher.group(1)
                            val markNumber = Integer.parseInt(mark)
                            val rest = choice.substring(mark.length + 1, choice.length)
                            val choiceContent = parsePattern(rest) ?: return null
                            choiceElements.add(ChoiceElement(choiceContent, markNumber))
                        } else {
                            val choiceContent = parsePattern(choice) ?: return null
                            choiceElements.add(ChoiceElement(choiceContent, 0))
                        }
                    }
                    elements.add(ChoiceGroup(choiceElements))
                }
                '<' -> {
                    val s = pattern.getEnclosedText('<', '>', i)
                    if (s == null) {
                        error("Unclosed regex group at index $i")
                        return null
                    }
                    if (textBuilder.isNotEmpty()) {
                        elements.add(TextElement(textBuilder.toString()))
                        textBuilder = StringBuilder("")
                    }
                    i += s.length + 1
                    val pat: Pattern
                    try {
                        pat = Pattern.compile(s)
                    } catch (e: PatternSyntaxException) {
                        error("Invalid regex : '$s'")
                        return null
                    }
                    elements.add(RegexGroup(pat))
                }
                '%' -> {
                    /*
                     * Can't use getEnclosedText as % acts for both opening and closing
                     * Moreover, there's no need of checking for nested stuff
                     */
                    val nextIndex = pattern.indexOf('%', i + 1)
                    if (nextIndex == -1) {
                        error("Unclosed variable declaration at index $i")
                        return null
                    }
                    if (textBuilder.isNotEmpty()) {
                        elements.add(TextElement(textBuilder.toString()))
                        textBuilder.setLength(0)
                    }
                    val s = pattern.substring(i + 1, nextIndex)
                    i = nextIndex
                    val m = VARIABLE_PATTERN.matcher(s)
                    if (!m.matches()) {
                        error("Invalid variable definition : '$s'")
                        return null
                    } else {
                        var acceptance: ExpressionElement.Acceptance = ExpressionElement.Acceptance.BOTH
                        if (m.group(1) != null) {
                            val acc = m.group(1)
                            acceptance = if (acc == "~") {
                                ExpressionElement.Acceptance.EXPRESSIONS_ONLY
                            } else {
                                ExpressionElement.Acceptance.LITERALS_ONLY
                            }
                        }
                        val typeString = m.group("types")
                        val types = typeString.split("/".toRegex()).dropLastWhile { it.isEmpty() }
                        val patternTypes = arrayListOf<PatternType<*>>()
                        for (type in types) {
                            val t = TypeManager.getPatternType(type)
                            if (t == null) {
                                error("Unknown type : $type")
                                return null
                            }
                            patternTypes.add(t)
                        }
                        elements.add(ExpressionElement(patternTypes, acceptance))
                    }
                }
                '\\' -> if (i == pattern.length - 1) {
                    error("Backslash sequence at the end of the pattern")
                    return null
                } else {
                    textBuilder.append(pattern[++i])
                }
                '|' -> {
                    val groups = pattern.split("(?<!\\\\)|".toRegex()).dropLastWhile { it.isEmpty() }
                    val choices = arrayListOf<ChoiceElement>()
                    for (choice in groups) {
                        val matcher = PARSE_MARK_PATTERN.matcher(choice)
                        if (matcher.matches()) {
                            val mark = matcher.group(1)
                            val markNumber = Integer.parseInt(mark)
                            val rest = choice.substring(mark.length + 1, choice.length)
                            val choiceContent = parsePattern(rest) ?: return null
                            choices.add(ChoiceElement(choiceContent, markNumber))
                        } else {
                            val choiceContent = parsePattern(choice) ?: return null
                            choices.add(ChoiceElement(choiceContent, 0))
                        }
                    }
                    elements.add(ChoiceGroup(choices))
                }
                else -> textBuilder.append(c)
            }
            i++
        }
        if (textBuilder.isNotEmpty()) {
            elements.add(TextElement(textBuilder.toString()))
            textBuilder.setLength(0)
        }
        return if (elements.size == 1) {
            elements[0]
        } else {
            CompoundElement(elements)
        }
    }

    private fun String.getEnclosedText(opening: Char, closing: Char, start: Int): String? {
        var n = 0
        var i = start
        while (i < length) {
            val c = get(i)
            if (c == '\\') {
                i++
            } else if (c == closing) {
                n--
                if (n == 0) {
                    return substring(start + 1, i) // We don't want the beginning bracket in there
                }
            } else if (c == opening) {
                n++
            }
            i++
        }
        return null
    }

    private fun error(error: String) {
        // TODO
    }

    companion object {
        private val PARSE_MARK_PATTERN = Pattern.compile("(\\d+?)\u00a6.*")
        private val VARIABLE_PATTERN = Pattern.compile("([*~])?(?<types>[\\w/]+)")
    }
}
