package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.minimumInteractiveComponentSize

// Represents the segmented digit configuration
data class SegmentDigit(val char: Char, val hasDecimal: Boolean)

// Operator states for the math backend
enum class CalcOperator {
    ADD, SUBTRACT, MULTIPLY, DIVIDE, NONE
}

class CalculatorViewModel : ViewModel() {
    var displayValue by mutableStateOf("0")
        private set

    var memoryValue by mutableStateOf(0.0)
        private set

    var isMemorySet by mutableStateOf(false)
        private set

    var activeOperator by mutableStateOf(CalcOperator.NONE)
        private set

    private var storedOperand: Double? = null
    private var isReadyForNewInput = false

    fun onButtonPress(btn: String) {
        if (displayValue == "Error" && btn != "AC") {
            // Must clear error first
            return
        }

        when (btn) {
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9" -> handleDigit(btn)
            "." -> handleDecimal()
            "AC" -> handleAllClear()
            "C" -> handleClear()
            "+/-" -> handleSignToggle()
            "%" -> handlePercentage()
            "+", "-", "*", "/" -> handleOperator(btn)
            "=" -> handleEquals()
            "MC" -> handleMemoryClear()
            "MR" -> handleMemoryRecall()
            "M+" -> handleMemoryPlus()
            "M-" -> handleMemoryMinus()
        }
    }

    private fun handleDigit(digit: String) {
        if (isReadyForNewInput || displayValue == "0") {
            displayValue = digit
            isReadyForNewInput = false
        } else {
            // Keep input under 10 digits for visual space
            if (displayValue.replace("-", "").replace(".", "").length < 10) {
                displayValue += digit
            }
        }
    }

    private fun handleDecimal() {
        if (isReadyForNewInput) {
            displayValue = "0."
            isReadyForNewInput = false
        } else {
            if (!displayValue.contains(".")) {
                displayValue += "."
            }
        }
    }

    private fun handleAllClear() {
        displayValue = "0"
        storedOperand = null
        activeOperator = CalcOperator.NONE
        isReadyForNewInput = false
    }

    private fun handleClear() {
        displayValue = "0"
    }

    private fun handleSignToggle() {
        if (displayValue == "0") return
        displayValue = if (displayValue.startsWith("-")) {
            displayValue.substring(1)
        } else {
            "-$displayValue"
        }
    }

    private fun handlePercentage() {
        val currentDouble = displayValue.toDoubleOrNull() ?: return
        val result = currentDouble / 100.0
        displayValue = formatResult(result)
        isReadyForNewInput = true
    }

    private fun handleOperator(opStr: String) {
        val currentDouble = displayValue.toDoubleOrNull() ?: return

        // Intermediate calculation on standard consecutive operators
        if (storedOperand != null && activeOperator != CalcOperator.NONE && !isReadyForNewInput) {
            val intermediateResult = calculate(storedOperand!!, currentDouble, activeOperator)
            if (intermediateResult == null) {
                displayValue = "Error"
                storedOperand = null
                activeOperator = CalcOperator.NONE
                return
            }
            displayValue = formatResult(intermediateResult)
            storedOperand = intermediateResult
        } else if (!isReadyForNewInput) {
            storedOperand = currentDouble
        }

        activeOperator = when (opStr) {
            "+" -> CalcOperator.ADD
            "-" -> CalcOperator.SUBTRACT
            "*" -> CalcOperator.MULTIPLY
            "/" -> CalcOperator.DIVIDE
            else -> CalcOperator.NONE
        }
        isReadyForNewInput = true
    }

    private fun handleEquals() {
        val currentDouble = displayValue.toDoubleOrNull() ?: return
        val operand = storedOperand

        if (operand != null && activeOperator != CalcOperator.NONE) {
            val result = calculate(operand, currentDouble, activeOperator)
            if (result == null) {
                displayValue = "Error"
            } else {
                displayValue = formatResult(result)
            }
            storedOperand = null
            activeOperator = CalcOperator.NONE
            isReadyForNewInput = true
        }
    }

    private fun calculate(operand1: Double, operand2: Double, op: CalcOperator): Double? {
        return when (op) {
            CalcOperator.ADD -> operand1 + operand2
            CalcOperator.SUBTRACT -> operand1 - operand2
            CalcOperator.MULTIPLY -> operand1 * operand2
            CalcOperator.DIVIDE -> {
                if (operand2 == 0.0) null else operand1 / operand2
            }
            CalcOperator.NONE -> operand2
        }
    }

    private fun handleMemoryClear() {
        memoryValue = 0.0
        isMemorySet = false
    }

    private fun handleMemoryRecall() {
        displayValue = formatResult(memoryValue)
        isReadyForNewInput = true
    }

    private fun handleMemoryPlus() {
        val currentDouble = displayValue.toDoubleOrNull() ?: return
        memoryValue += currentDouble
        isMemorySet = memoryValue != 0.0
        isReadyForNewInput = true
    }

    private fun handleMemoryMinus() {
        val currentDouble = displayValue.toDoubleOrNull() ?: return
        memoryValue -= currentDouble
        isMemorySet = memoryValue != 0.0
        isReadyForNewInput = true
    }

    private fun formatResult(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "Error"

        if (value > 9999999999.0 || value < -9999999999.0) {
            return "Error"
        }

        if (value == 0.0) return "0"

        val rawStr = value.toString()

        if (rawStr.endsWith(".0")) {
            val integerPart = rawStr.substring(0, rawStr.length - 2)
            if (integerPart.length <= 10) return integerPart
        }

        if (rawStr.length <= 10) {
            return rawStr
        }

        val formattedResult = String.format(java.util.Locale.US, "%.9f", value)
        var cleanStr = formattedResult
        if (cleanStr.contains(".")) {
            while (cleanStr.endsWith("0")) {
                cleanStr = cleanStr.substring(0, cleanStr.length - 1)
            }
            if (cleanStr.endsWith(".")) {
                cleanStr = cleanStr.substring(0, cleanStr.length - 1)
            }
        }

        if (cleanStr.length <= 10) {
            return cleanStr
        }

        val decimalPointIndex = cleanStr.indexOf('.')
        if (decimalPointIndex != -1 && decimalPointIndex < 10) {
            var truncated = cleanStr.substring(0, 10)
            if (truncated.endsWith(".")) {
                truncated = truncated.substring(0, 9)
            }
            return truncated
        }

        val sciFormatStr = String.format(java.util.Locale.US, "%.4e", value)
        if (sciFormatStr.length <= 10) {
            return sciFormatStr
        }

        return "Error"
    }

    fun getClearLabel(): String {
        return if (displayValue == "0") "AC" else "C"
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color(0xFF000000) // Deep dark black background
            ) { paddingValues ->
                CalculatorScreen(
                    modifier = Modifier
                        .padding(paddingValues)
                        .safeDrawingPadding()
                )
            }
        }
    }
}

@Composable
fun CalculatorScreen(modifier: Modifier = Modifier) {
    val viewModel: CalculatorViewModel = viewModel()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000)),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Display Section - fills the available upper area, aligning display to the bottom
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Title/Branding Header (Elegant and minimal)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CALCULATOR",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF444444),
                                letterSpacing = 2.sp
                            )
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF9F0A))
                            )
                            Text(
                                text = "PRO-GLOW",
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color(0xFF666666)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Retro 7-Segment Glass Display
                    SegmentedDisplay(
                        value = viewModel.displayValue,
                        isMemorySet = viewModel.isMemorySet,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Keypad Section - rounded-t-[40px] with #1C1C1E background as specified in layout patterns
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color(0xFF1C1C1E),
                        shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = Color(0xFF2C2C2E),
                        shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp)
                    )
                    .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ROW 1: Memory Operations Row (smaller height, subtle colors as in Professional Polish)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf("MC", "MR", "M-", "M+").forEach { memoryKey ->
                        CalcButton(
                            label = memoryKey,
                            backgroundColor = Color(0xFF2C2C2E),
                            contentColor = if (viewModel.isMemorySet && memoryKey == "MR") Color(0xFFFF9F0A) else Color(0xFFACACAC),
                            onClick = { viewModel.onButtonPress(memoryKey) },
                            isSmall = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // ROW 2: AC, +/-, %, /
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val clearLabel = viewModel.getClearLabel()
                    CalcButton(
                        label = clearLabel,
                        backgroundColor = Color(0xFFA5A5A5),
                        contentColor = Color(0xFF000000),
                        onClick = { viewModel.onButtonPress(clearLabel) },
                        modifier = Modifier.weight(1f)
                    )

                    CalcButton(
                        label = "+/-",
                        backgroundColor = Color(0xFFA5A5A5),
                        contentColor = Color(0xFF000000),
                        onClick = { viewModel.onButtonPress("+/-") },
                        modifier = Modifier.weight(1f)
                    )

                    CalcButton(
                        label = "%",
                        backgroundColor = Color(0xFFA5A5A5),
                        contentColor = Color(0xFF000000),
                        onClick = { viewModel.onButtonPress("%") },
                        modifier = Modifier.weight(1f)
                    )

                    val isDivideActive = viewModel.activeOperator == CalcOperator.DIVIDE
                    CalcButton(
                        label = "/",
                        backgroundColor = if (isDivideActive) Color.White else Color(0xFFFF9F0A),
                        contentColor = if (isDivideActive) Color(0xFFFF9F0A) else Color.White,
                        onClick = { viewModel.onButtonPress("/") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // ROW 3: Numbers 7, 8, 9 & Multiplier
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf("7", "8", "9").forEach { num ->
                        CalcButton(
                            label = num,
                            backgroundColor = Color(0xFF333333),
                            contentColor = Color.White,
                            onClick = { viewModel.onButtonPress(num) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    val isMulActive = viewModel.activeOperator == CalcOperator.MULTIPLY
                    CalcButton(
                        label = "*",
                        backgroundColor = if (isMulActive) Color.White else Color(0xFFFF9F0A),
                        contentColor = if (isMulActive) Color(0xFFFF9F0A) else Color.White,
                        onClick = { viewModel.onButtonPress("*") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // ROW 4: Numbers 4, 5, 6 & Subtract
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf("4", "5", "6").forEach { num ->
                        CalcButton(
                            label = num,
                            backgroundColor = Color(0xFF333333),
                            contentColor = Color.White,
                            onClick = { viewModel.onButtonPress(num) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    val isSubActive = viewModel.activeOperator == CalcOperator.SUBTRACT
                    CalcButton(
                        label = "-",
                        backgroundColor = if (isSubActive) Color.White else Color(0xFFFF9F0A),
                        contentColor = if (isSubActive) Color(0xFFFF9F0A) else Color.White,
                        onClick = { viewModel.onButtonPress("-") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // ROW 5: Numbers 1, 2, 3 & Add
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf("1", "2", "3").forEach { num ->
                        CalcButton(
                            label = num,
                            backgroundColor = Color(0xFF333333),
                            contentColor = Color.White,
                            onClick = { viewModel.onButtonPress(num) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    val isAddActive = viewModel.activeOperator == CalcOperator.ADD
                    CalcButton(
                        label = "+",
                        backgroundColor = if (isAddActive) Color.White else Color(0xFFFF9F0A),
                        contentColor = if (isAddActive) Color(0xFFFF9F0A) else Color.White,
                        onClick = { viewModel.onButtonPress("+") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // ROW 6: Width Span Zero, Decimal, Equals
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Double width 0 button modeled precisely on Apple calculator layout
                    CalcButton(
                        label = "0",
                        backgroundColor = Color(0xFF333333),
                        contentColor = Color.White,
                        onClick = { viewModel.onButtonPress("0") },
                        isPill = true,
                        modifier = Modifier.weight(2f)
                    )

                    CalcButton(
                        label = ".",
                        backgroundColor = Color(0xFF333333),
                        contentColor = Color.White,
                        onClick = { viewModel.onButtonPress(".") },
                        modifier = Modifier.weight(1f)
                    )

                    CalcButton(
                        label = "=",
                        backgroundColor = Color(0xFFFF9F0A),
                        contentColor = Color.White,
                        onClick = { viewModel.onButtonPress("=") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Elegant bottom Home Indicator representation
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 4.dp)
                        .size(width = 120.dp, height = 5.dp)
                        .background(Color(0x33FFFFFF), RoundedCornerShape(50))
                        .align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Composable
fun SegmentedDisplay(
    value: String,
    isMemorySet: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF080808)) // Dark premium glass visor
            .border(2.dp, Color(0xFF1C1C1E), RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Memory & Diagnostics header indicators row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isMemorySet) Color(0xFFFF9F0A) else Color(0xFF261208))
                    )
                    Text(
                        text = "MEM",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (isMemorySet) Color(0xFFFF9F0A) else Color(0xFF2E2E2E)
                        )
                    )
                }

                Text(
                    text = "SOLID STATE 10D-LED",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = Color(0xFF444444)
                    )
                )
            }

            // Visual grid of the 10 digits
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val parsedDigits = parseDisplayString(value)
                val totalSlots = 10

                // Pad with blank digits to maintain 10-digit pocket LED appearance
                val paddedDigits = if (parsedDigits.size < totalSlots) {
                    List(totalSlots - parsedDigits.size) { SegmentDigit(' ', false) } + parsedDigits
                } else {
                    parsedDigits.takeLast(totalSlots)
                }

                for (digit in paddedDigits) {
                    SegmentDigitView(
                        digit = digit.char,
                        hasDecimal = digit.hasDecimal,
                        activeColor = Color(0xFFFF9F0A), // Rich neon glow matching core orange accent
                        inactiveColor = Color(0xFF22140A), // Low-luminance warm inactive shadow
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(0.55f)
                    )
                }
            }
        }
    }
}

@Composable
fun SegmentDigitView(
    digit: Char,
    hasDecimal: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val t = w * 0.12f // thickness
        val spacing = w * 0.02f // segment gaps
        val midY = h / 2f
        val topVHeight = midY - 1.5f * t - 2f * spacing
        val botVHeight = midY - 1.5f * t - 2f * spacing

        // Segments are mapped as boolean checks based on current Char
        // SEGMENT A - Top
        val isA = isSegmentActive(digit, 'A')
        drawRoundRect(
            color = if (isA) activeColor else inactiveColor,
            topLeft = Offset(t + spacing, spacing),
            size = Size(w - 2f * (t + spacing), t),
            cornerRadius = CornerRadius(t / 2f, t / 2f)
        )

        // SEGMENT F - Top Left
        val isF = isSegmentActive(digit, 'F')
        drawRoundRect(
            color = if (isF) activeColor else inactiveColor,
            topLeft = Offset(spacing, t + spacing),
            size = Size(t, topVHeight),
            cornerRadius = CornerRadius(t / 2f, t / 2f)
        )

        // SEGMENT B - Top Right
        val isB = isSegmentActive(digit, 'B')
        drawRoundRect(
            color = if (isB) activeColor else inactiveColor,
            topLeft = Offset(w - t - spacing, t + spacing),
            size = Size(t, topVHeight),
            cornerRadius = CornerRadius(t / 2f, t / 2f)
        )

        // SEGMENT G - Middle
        val isG = isSegmentActive(digit, 'G')
        drawRoundRect(
            color = if (isG) activeColor else inactiveColor,
            topLeft = Offset(t + spacing, midY - t / 2f),
            size = Size(w - 2f * (t + spacing), t),
            cornerRadius = CornerRadius(t / 2f, t / 2f)
        )

        // SEGMENT E - Bottom Left
        val isE = isSegmentActive(digit, 'E')
        drawRoundRect(
            color = if (isE) activeColor else inactiveColor,
            topLeft = Offset(spacing, midY + t / 2f + spacing),
            size = Size(t, botVHeight),
            cornerRadius = CornerRadius(t / 2f, t / 2f)
        )

        // SEGMENT C - Bottom Right
        val isC = isSegmentActive(digit, 'C')
        drawRoundRect(
            color = if (isC) activeColor else inactiveColor,
            topLeft = Offset(w - t - spacing, midY + t / 2f + spacing),
            size = Size(t, botVHeight),
            cornerRadius = CornerRadius(t / 2f, t / 2f)
        )

        // SEGMENT D - Bottom
        val isD = isSegmentActive(digit, 'D')
        drawRoundRect(
            color = if (isD) activeColor else inactiveColor,
            topLeft = Offset(t + spacing, h - t - spacing),
            size = Size(w - 2f * (t + spacing), t),
            cornerRadius = CornerRadius(t / 2f, t / 2f)
        )

        // Decimal dot element representation
        drawCircle(
            color = if (hasDecimal) activeColor else inactiveColor,
            radius = t * 0.5f,
            center = Offset(w - t * 0.5f, h - t * 0.5f)
        )
    }
}

@Composable
fun CalcButton(
    label: String,
    backgroundColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPill: Boolean = false,
    isSmall: Boolean = false
) {
    val fontSize = when {
        isSmall -> 14.sp
        label.length > 2 -> 16.sp
        label.length > 1 -> 20.sp
        else -> 28.sp
    }

    val aspect = if (isPill) 2.2f else if (isSmall) 2.1f else 1f
    val shape = if (isPill || isSmall) RoundedCornerShape(percent = 50) else CircleShape

    Box(
        modifier = modifier
            .testTag("btn_$label")
            .aspectRatio(aspect)
            .clip(shape)
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = if (isSmall) FontWeight.Bold else FontWeight.Medium,
                fontSize = fontSize,
                color = contentColor
            )
        )
    }
}

// Convert display value string into individual segment configurations
fun parseDisplayString(input: String): List<SegmentDigit> {
    val result = mutableListOf<SegmentDigit>()
    var i = 0
    while (i < input.length) {
        val char = input[i]
        if (char == '.') {
            if (result.isEmpty()) {
                result.add(SegmentDigit('0', true))
            } else {
                val last = result.removeAt(result.size - 1)
                result.add(last.copy(hasDecimal = true))
            }
        } else {
            val nextIsDecimal = (i + 1 < input.length && input[i + 1] == '.')
            result.add(SegmentDigit(char, nextIsDecimal))
            if (nextIsDecimal) {
                i++
            }
        }
        i++
    }
    return result
}

// Helper mapping standard symbols to standard 7-segment configurations
fun isSegmentActive(char: Char, segment: Char): Boolean {
    return when (char.uppercaseChar()) {
        '0' -> segment in listOf('A', 'B', 'C', 'D', 'E', 'F')
        '1' -> segment in listOf('B', 'C')
        '2' -> segment in listOf('A', 'B', 'G', 'E', 'D')
        '3' -> segment in listOf('A', 'B', 'G', 'C', 'D')
        '4' -> segment in listOf('F', 'G', 'B', 'C')
        '5' -> segment in listOf('A', 'F', 'G', 'C', 'D')
        '6' -> segment in listOf('A', 'F', 'G', 'E', 'C', 'D')
        '7' -> segment in listOf('A', 'B', 'C')
        '8' -> segment in listOf('A', 'B', 'C', 'D', 'E', 'F', 'G')
        '9' -> segment in listOf('A', 'B', 'C', 'D', 'F', 'G')
        '-' -> segment == 'G'
        'E' -> segment in listOf('A', 'D', 'E', 'F', 'G')
        'R' -> segment in listOf('E', 'G')
        'O' -> segment in listOf('C', 'D', 'E', 'G')
        'L' -> segment in listOf('D', 'E', 'F')
        'H' -> segment in listOf('F', 'G', 'B', 'C', 'E')
        'P' -> segment in listOf('A', 'B', 'F', 'G', 'E')
        else -> false
    }
}
