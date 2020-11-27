//TODO: Just for demo reasons.. (quick&dirty) this is not a "React-way" to do stuff...
import React, { Component } from 'react';

class RealTimeChart extends Component {
  constructor(props) {
    super(props);

    this.data = [];
    this.totalPoints = 110;
    this.updateInterval = 320;
    this.realtime = 'on';
  }

  getRandomData() {
    if (this.data.length > 0) this.data = this.data.slice(1);

    while (this.data.length < this.totalPoints) {
      var prev = this.data.length > 0 ? this.data[this.data.length - 1] : 50,
        y = prev + Math.random() * 10 - 5;
      if (y < 0) {
        y = 0;
      } else if (y > 100) {
        y = 100;
      }

      this.data.push(y);
    }

    var res = [];
    for (var i = 0; i < this.data.length; ++i) {
      res.push([i, this.data[i]]);
    }

    return res;
  }

  componentDidMount() {
    const $ = window.$;

    var opt = {
      series: {
        shadowSize: 0,
        color: 'rgb(0, 188, 212)',
      },
      grid: {
        borderColor: '#f3f3f3',
        borderWidth: 1,
        tickColor: '#f3f3f3',
      },
      lines: {
        fill: true,
      },
      yaxis: {
        min: 0,
        max: 100,
      },
      xaxis: {
        min: 0,
        max: 100,
      },
    };

    var plot = $.plot('#real_time_chart', [this.getRandomData()], opt);

    function updateRealTime() {
      plot.setData([this.getRandomData()]);
      plot.draw();

      var timeout;
      if (this.realtime === 'on') {
        timeout = setTimeout(updateRealTime.bind(this), this.updateInterval);
      } else {
        clearTimeout(timeout);
      }
    }

    updateRealTime.bind(this)();

    // $('#realtime').on('change', function () {
    //    realtime = this.checked ? 'on' : 'off';
    //   updateRealTime();
    // });
  }

  render() {
    const styles = {
      width: '100%',
      height: '275px',
    };
    return (
      <div
        id="real_time_chart"
        className="dashboard-flot-chart"
        ref="real_time_chart"
        style={styles}
      />
    );
  }
}

export default RealTimeChart;
