package com.chuckerteam.chucker.api

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
public fun ChuckerDesktopScreen() {
    Column(modifier = Modifier.fillMaxSize()) {
        BasicText("Hello world")
    }
}
