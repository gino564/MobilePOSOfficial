package com.project.dba_delatorre_dometita_ramirez_tan

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class frm_RegViewModel: ViewModel() {
    var dometita_lname by  mutableStateOf("")
        private set
    var dometita_fname by  mutableStateOf("")
        private set
    var dometita_mname by  mutableStateOf("")
        private set
    var dometita_username by  mutableStateOf("")
        private set
    var dometita_password by  mutableStateOf("")
        private set

    fun txtlnameData(value: String){
        dometita_lname = value
    }
    fun txtfnameData(value: String){
        dometita_fname = value
    }
    fun txtmnameData(value: String){
        dometita_mname = value
    }
    fun txtusernameData(value: String){
        dometita_username = value
    }
    fun txtpasswordData(value: String){
        dometita_password = value
    }


}