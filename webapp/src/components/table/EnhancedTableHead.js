import React from 'react';
import PropTypes from 'prop-types';

// material-ui components
import { withStyles } from '@material-ui/core/styles';
import TableHead from '@material-ui/core/TableHead';
import TableCell from '@material-ui/core/TableCell';
import TableRow from '@material-ui/core/TableRow';
import TableSortLabel from '@material-ui/core/TableSortLabel';
import Tooltip from '@material-ui/core/Tooltip';
import TextField from '@material-ui/core/TextField';

const styles = (theme) => ({
  searchBar: {
    color: '#777777',
    transitionDuration: '0.6s',
    transitionProperty: 'opacity',
    border: 0,
    outline: 0,
    fontSize: 14,
    fontWeight: 300,
    width: '100%',
    textIndent: 3,
    cursor: 'text',
  },
  tableCellDense: {
    paddingRight: '5px',
  },
});

class EnhancedTableHead extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      opacity: 0,
      disabled: true,
      filterStrings: this.clearFilterValues(),
    };
  }

  clearFilterValues = (event) => {
    const filterStrings = {};
    this.props.columnData.forEach(function (column) {
      filterStrings[column.id] = '';
    });

    return filterStrings;
  };

  componentDidMount() {
    this.props.onRef(this);
  }
  componentWillUnmount() {
    this.props.onRef(null);
  }

  createSortHandler = (property) => (event) => {
    this.props.onRequestSort(event, property);
  };

  onSearch = (property) => (event) => {
    this.props.onSearch(event.target.value, property);
    const filterStrings = this.state.filterStrings;
    filterStrings[property] = event.target.value;
    this.setState({ filterStrings });
  };

  // called from parent component via ref->onFilter
  onFilter = (event) => {
    // const searchbarStyle = Object.assign({}, this.state.searchbarStyle, {});
    // searchbarStyle.opacity = (searchbarStyle.opacity === 0 ? 1 : 0);
    const opacity = this.state.opacity === 0 ? 1 : 0;
    const disabled = this.state.disabled === false ? true : false;
    const filterStrings = this.clearFilterValues();
    this.props.clearSearch();

    this.setState({ opacity, disabled, filterStrings });
  };

  render() {
    const { order, orderBy, columnData, classes } = this.props;

    return (
      <TableHead>
        <TableRow>
          {columnData.map((column) => {
            return (
              <TableCell
                key={column.id}
                align={column.numeric ? 'right' : 'inherit'}
                //numeric={column.numeric}
                className={
                  column.disablePadding ? classes.tableCellDense : null
                }
              >
                <Tooltip
                  title="Sort"
                  placement={column.numeric ? 'bottom-end' : 'bottom-start'}
                  enterDelay={300}
                >
                  <TableSortLabel
                    active={orderBy === column.id}
                    direction={order}
                    onClick={this.createSortHandler(column.id)}
                  >
                    {column.label}
                  </TableSortLabel>
                </Tooltip>
                <TextField
                  id={column.id}
                  type="search"
                  placeholder="Search"
                  className={classes.searchBar}
                  style={{ opacity: this.state.opacity }}
                  disabled={this.state.disabled}
                  onChange={this.onSearch(column.id)}
                  value={this.state.filterStrings[column.id]}
                />
              </TableCell>
            );
          }, this)}
          <TableCell key="actions"> </TableCell>
        </TableRow>
      </TableHead>
    );
  }
}

EnhancedTableHead.propTypes = {
  classes: PropTypes.object.isRequired,
  onRequestSort: PropTypes.func.isRequired,
  order: PropTypes.string.isRequired,
  orderBy: PropTypes.string.isRequired,
  columnData: PropTypes.array.isRequired,
  onSearch: PropTypes.func.isRequired,
  clearSearch: PropTypes.func.isRequired,
  onRef: PropTypes.func.isRequired,
};

export default withStyles(styles)(EnhancedTableHead);
