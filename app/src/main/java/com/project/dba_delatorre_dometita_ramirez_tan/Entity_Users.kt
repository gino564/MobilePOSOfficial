package com.project.dba_delatorre_dometita_ramirez_tan

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tbl_Users")
data class Entity_Users(
    @PrimaryKey(autoGenerate = true) val Entity_id:Int = 0,
    val Entity_lname: String = "",
    val Entity_fname: String = "",
    val Entity_mname: String = "",
    val Entity_username: String = "",
    val Entity_password: String = "",
    val profileImageUri: String? = null
)
