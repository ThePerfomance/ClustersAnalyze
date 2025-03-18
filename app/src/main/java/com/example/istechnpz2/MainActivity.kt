package com.example.istechnpz2

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.ScatterChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.ScatterData
import com.github.mikephil.charting.data.ScatterDataSet
import com.github.mikephil.charting.interfaces.datasets.IScatterDataSet
import kotlin.math.pow
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {

    private lateinit var chartBefore: ScatterChart
    private lateinit var chartAfter: ScatterChart
    private lateinit var clusterSeekBar: SeekBar
    private lateinit var clusterCountText: TextView
    private lateinit var runClusteringButton: Button
    private lateinit var regenerateButton: Button
    private lateinit var pointsInput: EditText

    private var dataPoints = listOf<Entry>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Инициализация элементов интерфейса
        chartBefore = findViewById(R.id.chartBefore)
        chartAfter = findViewById(R.id.chartAfter)
        clusterSeekBar = findViewById(R.id.clusterSeekBar)
        clusterCountText = findViewById(R.id.clusterCountText)
        runClusteringButton = findViewById(R.id.runClusteringButton)
        regenerateButton = findViewById(R.id.regenerateButton)
        pointsInput = findViewById(R.id.pointsInput)

        // Обновление текста при изменении SeekBar
        clusterSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                clusterCountText.text = (progress + 1).toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Перегенерация точек
        regenerateButton.setOnClickListener {
            val pointsCount = pointsInput.text.toString().toIntOrNull() ?: 100
            dataPoints = generateRandomData(pointsCount, 0f, 10f)
            setupScatterChart(chartBefore, listOf(dataPoints), "Before Clustering")
            chartAfter.clear()
        }

        // Запуск кластеризации
        runClusteringButton.setOnClickListener {
            val k = clusterSeekBar.progress + 1 // Получаем количество кластеров из SeekBar
            val clusters = performKMeans(dataPoints, k, maxIterations = 10)

            // Построение графика "после кластеризации"
            setupScatterChart(chartAfter, clusters, "After Clustering")
        }

        // Генерация начальных данных
        dataPoints = generateRandomData(100, 0f, 10f)
        setupScatterChart(chartBefore, listOf(dataPoints), "Before Clustering")
    }

    /**
     * Генерация случайных точек данных.
     */
    private fun generateRandomData(count: Int, min: Float, max: Float): List<Entry> {
        val random = java.util.Random()
        return List(count) {
            Entry(
                min + random.nextFloat() * (max - min),
                min + random.nextFloat() * (max - min)
            )
        }
    }

    /**
     * Настройка ScatterChart.
     */
    private fun setupScatterChart(chart: ScatterChart, clusters: List<List<Entry>>, title: String) {
            val colors = listOf(
                Color.RED,
                Color.BLUE,
                Color.GREEN,
                Color.YELLOW,
                Color.MAGENTA,
                Color.CYAN,
                Color.BLACK,
                Color.GRAY,
                Color.parseColor("#FFA500"), // Оранжевый
                Color.parseColor("#800080"), // Пурпурный
                Color.parseColor("#008000"), // Темно-зеленый
                Color.parseColor("#FF4500"), // Оранжево-красный
                Color.parseColor("#00FFFF"), // Бирюзовый
                Color.parseColor("#FF69B4"), // Ярко-розовый
                Color.parseColor("#8B4513"), // Коричневый
                Color.parseColor("#2E8B57"), // Изумрудный
                Color.parseColor("#FFD700"), // Золотой
                Color.parseColor("#FF1493"), // Глубокий розовый
                Color.parseColor("#ADFF2F"), // Зеленый лайм
                Color.parseColor("#1E90FF"), // Ярко-синий
                Color.parseColor("#FF6347"), // Томатный
                Color.parseColor("#9400D3"), // Темно-фиолетовый
                Color.parseColor("#32CD32"), // Лайм-зеленый
                Color.parseColor("#FF8C00"), // Темно-оранжевый
                Color.parseColor("#4B0082"), // Индиго
                Color.parseColor("#FA8072"), // Лососевый
                Color.parseColor("#00FA9A"), // Средне-весенний зеленый
                Color.parseColor("#DB7093")  // Светло-пурпурный
            )
        val dataSets = mutableListOf<ScatterDataSet>()

        // Создаем отдельный ScatterDataSet для каждого кластера
        for ((index, cluster) in clusters.withIndex()) {
            val dataSet = ScatterDataSet(cluster, "Cluster ${index + 1}").apply {
                color = colors.getOrElse(index) { Color.GRAY } // Используем цвет из списка или серый
                valueTextColor = Color.BLACK
                valueTextSize = 10f
                setScatterShape(ScatterChart.ScatterShape.CIRCLE)
            }
            dataSets.add(dataSet)
        }

        // Объединяем все данные в один ScatterData
        val scatterData = ScatterData(dataSets as List<IScatterDataSet>?)
        chart.apply {
            this.data = scatterData
            description.text = title
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            axisLeft.axisMinimum = 0f
            axisRight.axisMinimum = 0f
            legend.isEnabled = true // Включаем легенду для отображения кластеров
            invalidate()
        }
    }

    /**
     * Реализация алгоритма k-means.
     */
    private fun performKMeans(points: List<Entry>, k: Int, maxIterations: Int): List<List<Entry>> {
        val random = java.util.Random()
        val centroids = List(k) { points[random.nextInt(points.size)] }

        var currentCentroids = centroids
        var clusters: List<MutableList<Entry>> = List(k) { mutableListOf() }

        for (iteration in 0 until maxIterations) {
            // Очистка кластеров
            clusters = List(k) { mutableListOf() }

            // Распределение точек по кластерам
            for (point in points) {
                val closestCentroid = currentCentroids.minByOrNull { centroid ->
                    distance(point, centroid)
                }
                val closestCentroidIndex = currentCentroids.indexOf(closestCentroid)
                clusters[closestCentroidIndex].add(point)
            }

            // Обновление центроидов
            val newCentroids = clusters.map { cluster ->
                if (cluster.isEmpty()) {
                    currentCentroids.random()
                } else {
                    Entry(
                        cluster.map { it.x }.average().toFloat(),
                        cluster.map { it.y }.average().toFloat()
                    )
                }
            }

            // Проверка на сходимость
            if (newCentroids == currentCentroids) break
            currentCentroids = newCentroids
        }

        return clusters
    }

    /**
     * Расчет евклидова расстояния между двумя точками.
     */
    private fun distance(p1: Entry, p2: Entry): Float {
        return sqrt((p1.x - p2.x).pow(2) + (p1.y - p2.y).pow(2))
    }
}