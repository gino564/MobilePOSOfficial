package com.project.dba_delatorre_dometita_ramirez_tan

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter

class SalesReportViewModel(private val repository: SalesReportRepository) : ViewModel() {

    var salesList by mutableStateOf<List<Entity_SalesReport>>(emptyList())
        private set

    init {
        getAllSales()
    }

    fun getAllSales() {
        viewModelScope.launch {
            salesList = repository.getAll()
        }
    }

    fun insertSale(sale: Entity_SalesReport) {
        viewModelScope.launch {
            repository.insert(sale)
            getAllSales()
        }
    }

    fun clearSales() {
        viewModelScope.launch {
            repository.clear()
            getAllSales()
        }
    }

    fun filterSalesByDate(date: String) {
        viewModelScope.launch {
            salesList = repository.getByDate(date)
        }
    }

    fun filterSalesByRange(startDate: String, endDate: String) {
        viewModelScope.launch {
            salesList = repository.getSalesBetweenDates(startDate, endDate)
        }
    }

    private val _topSales = mutableStateOf<List<TopSalesItem>>(emptyList())
    val topSales: State<List<TopSalesItem>> = _topSales

    fun loadTopSales() {
        viewModelScope.launch {
            _topSales.value = repository.getTopSales()
        }
    }

    var totalSold by mutableStateOf(0)
        private set

    var totalRevenue by mutableStateOf(0.0)
        private set

    fun filterByPeriod(period: String) {
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        val today = LocalDate.now()

        val startDate: String
        val endDate: String

        when (period) {
            "Today" -> {
                val start = today.atStartOfDay()
                val end = start.plusDays(1)
                startDate = start.format(dateTimeFormatter)
                endDate = end.format(dateTimeFormatter)
            }
            "Week" -> {
                startDate = today.minusDays(6).format(dateFormatter)
                endDate = today.format(dateFormatter)
            }
            "Month" -> {
                startDate = today.withDayOfMonth(1).format(dateFormatter)
                endDate = today.format(dateFormatter)
            }
            else -> return
        }

        viewModelScope.launch {
            salesList = repository.getSalesBetweenDates(startDate, endDate)
            _topSales.value = repository.getTopSalesByDate(startDate, endDate)
            totalSold = repository.getTotalQuantitySoldBetween(startDate, endDate)
            totalRevenue = repository.getTotalRevenueBetween(startDate, endDate)
        }
    }

    fun loadTopSalesByDate(startDate: String, endDate: String) {
        viewModelScope.launch {
            _topSales.value = repository.getTopSalesBetween(startDate, endDate)
        }
    }
}

class SalesReportViewModelFactory(private val repository: SalesReportRepository) :
    ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SalesReportViewModel(repository) as T
    }
}
