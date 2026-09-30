package pt.ipc_app.ui.theme

import androidx.compose.material.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.unit.sp
import pt.ipc_app.R


// https://www.fontspace.com/monday-feelings-font-f88501
private val AppFont = FontFamily(
    Font(R.font.nunitoregular)
)

// Set of Material typography styles for healthcare app
val Typography = Typography(
    h1 = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp,
        fontSize = 32.sp
    ),
    h2 = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp,
        fontSize = 28.sp
    ),
    h3 = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
        fontSize = 24.sp
    ),
    h4 = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
        fontSize = 20.sp
    ),
    h5 = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
        fontSize = 18.sp
    ),
    h6 = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
        fontSize = 16.sp
    ),
    subtitle1 = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
        fontSize = 16.sp
    ),
    subtitle2 = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
        fontSize = 14.sp
    ),
    body1 = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
        fontSize = 16.sp
    ),
    body2 = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
        fontSize = 14.sp
    ),
    button = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp,
        fontSize = 16.sp,
    ),
    caption = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
        fontSize = 12.sp
    ),
    overline = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.sp,
        fontSize = 10.sp
    )
)
