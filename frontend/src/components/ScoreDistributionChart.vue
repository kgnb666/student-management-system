<template>
  <div ref="chartRef" class="chart-box"></div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts/core'
import { BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

// 只注册图表实际用到的模块，避免引入完整 ECharts。
echarts.use([BarChart, GridComponent, TooltipComponent, CanvasRenderer])

const props = defineProps({
  distribution: {
    type: Array,
    default: () => []
  }
})

const chartRef = ref()
let chart

function renderChart() {
  if (!chart) {
    return
  }
  const data = props.distribution || []
  chart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    grid: {
      left: 50,
      right: 30,
      top: 30,
      bottom: 40
    },
    xAxis: {
      type: 'category',
      data: data.map((item) => item.name)
    },
    yAxis: {
      type: 'value',
      minInterval: 1
    },
    series: [
      {
        name: '人数',
        type: 'bar',
        barWidth: 48,
        data: data.map((item) => item.value),
        itemStyle: {
          color: '#409eff',
          borderRadius: [5, 5, 0, 0]
        },
        label: {
          show: true,
          position: 'top'
        }
      }
    ]
  })
}

function resizeChart() {
  chart?.resize()
}

onMounted(async () => {
  await nextTick()
  chart = echarts.init(chartRef.value)
  renderChart()
  window.addEventListener('resize', resizeChart)
})

watch(
  () => props.distribution,
  () => renderChart(),
  { deep: true }
)

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart)
  chart?.dispose()
})
</script>
