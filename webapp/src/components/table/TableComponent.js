import React from 'react';
import PropTypes from 'prop-types';
import { toast } from 'react-toastify';

// material-ui components
import { withStyles } from '@material-ui/core/styles';
import Paper from '@material-ui/core/Paper';
import Table from '@material-ui/core/Table';
import TableBody from '@material-ui/core/TableBody';
import TableCell from '@material-ui/core/TableCell';
import TableFooter from '@material-ui/core/TableFooter';
import TablePagination from '@material-ui/core/TablePagination';
import TableRow from '@material-ui/core/TableRow';

// table components
import EnhancedTableHead from './EnhancedTableHead';
import EnhancedTableToolbar from './EnhancedTableToolbar';

//controller
import * as Parser from '../../services/handler/parser';

const styles = (theme) => ({
  root: {
    marginTop: theme.spacing(3),
  },
  table: {},
  tableWrapper: {
    overflowX: 'auto',
  },
  tableCellDense: {
    paddingRight: '5px',
  },
  toClipboard: {
    display: 'none',
  },
});

//TODO: withStyles
const searchStyle = {
  opacity: 0,
  color: '#777777',
  transitionDuration: '0.6s',
  transitionProperty: 'opacity',
  border: 0,
  outline: 0,
  fontSize: 16,
  width: '100%',
  textIndent: 3,
  cursor: 'text',
};

const ORDER_ASC = 'asc';
const ORDER_DESC = 'desc';

class TableComponent extends React.Component {
  constructor(props, context) {
    super(props, context);

    this.state = {
      order: ORDER_ASC,
      orderBy: this.props.orderBy,
      data: this.props.data,
      origData: this.props.data,
      page: 0,
      rowsPerPage: 10,
      searchStyle: searchStyle,
      isDataReloaded: false,
    };

    this.toggleFilter = this.toggleFilter.bind(this);
    this.onCellClick = this.onCellClick.bind(this);
  }

  componentDidUpdate(prevProps) {
    if (
      this.props.data.length !== prevProps.data.length ||
      this.state.isDataReloaded
    ) {
      // call this for ordering the table on initial load
      let data = this.sort(
        this.props.data,
        this.state.order,
        this.props.orderBy
      );
      this.setState({
        data: data,
        origData: data,
        isDataReloaded: false,
      });
    }
  }

  sort(data, order, orderBy) {
    return order === ORDER_DESC
      ? data.sort((a, b) => {
          return a[orderBy] < b[orderBy] ? -1 : 1;
        })
      : data.sort((a, b) => {
          return a[orderBy] < b[orderBy] ? 1 : -1;
        });
  }

  handleRequestSort = (event, orderBy) => {
    let order = ORDER_DESC;
    if (this.state.orderBy === orderBy && this.state.order === ORDER_DESC) {
      order = ORDER_ASC;
    }

    let data = this.sort(this.state.data, order, orderBy);
    this.setState({ data, order, orderBy });
  };

  searchData = (searchstring, prop) => {
    var selection = this.search(prop, searchstring);
    this.setState({ data: selection });
  };

  clearSearch = () => {
    this.setState({ data: this.state.origData });
  };

  search = (key, word) => {
    const data = this.state.origData;

    let regex;
    try {
      regex = new RegExp(word, 'i');
    } catch (err) {
      // here a SyntaxError can be thrown when 'word' is not a valid Regex
      regex = null;
    }

    if (word.length < 1 || regex === null) return data;

    const res = [];
    const keys = key.split('|'); //TODO: why split?

    data.forEach((item) => {
      for (let i = 0; i < keys.length; i += 1) {
        var str =
          keys[i] === 'modifiedAt'
            ? Parser.SetVariableFormat(String(item[keys[i]]), 'date')
            : String(item[keys[i]]);
        if (str.match(regex)) {
          res.push(item);
          break;
        }
      }
    });

    return res;
  };

  handleChangePage = (event, page) => {
    this.setState({ page });
  };

  handleChangeRowsPerPage = (event) => {
    this.setState({ rowsPerPage: event.target.value });
  };

  toggleFilter = (event) => {
    this.tablehead.onFilter();
  };

  onCellClick = (itemKey) => {
    Parser.setClipboardText(itemKey);
    if (toast.isActive(this.toastId)) {
      toast.dismiss(this.toastId);
    }
    this.toastId = toast.info('Copied to Clipboard');
  };

  reload = () => {
    this.props.onReload();
    this.setState({ isDataReloaded: true });
  };

  render() {
    const {
      classes,
      columns,
      title,
      primaryActionBtn,
      enableCellClick,
    } = this.props;
    const { data, order, orderBy, rowsPerPage, page } = this.state;
    return (
      <Paper className={classes.root}>
        <EnhancedTableToolbar
          title={title}
          toggleFilter={this.toggleFilter}
          onReload={this.reload}
        />
        <div className={classes.tableWrapper}>
          <Table className={classes.table}>
            <EnhancedTableHead
              onRef={(ref) => (this.tablehead = ref)}
              order={order}
              orderBy={orderBy}
              onRequestSort={this.handleRequestSort}
              columnData={columns}
              onSearch={this.searchData}
              clearSearch={this.clearSearch}
            />
            <TableBody>
              {data
                .slice(page * rowsPerPage, page * rowsPerPage + rowsPerPage)
                .map((item) => {
                  const key =
                    item.name + '_' + Date.now() + '_' + Math.random();
                  return (
                    <TableRow key={key} hover tabIndex={-1}>
                      {columns.map((column) => {
                        return (
                          <TableCell
                            onClick={
                              enableCellClick
                                ? () => this.onCellClick(item.key)
                                : null
                            }
                            className={
                              column.disablePadding
                                ? classes.tableCellDense
                                : null
                            }
                            key={key + '_' + column.id}
                            align={column.numeric ? 'right' : 'inherit'}
                          >
                            {Parser.SetVariableFormat(
                              item[column.id],
                              column.type
                            )}
                          </TableCell>
                        );
                      })}

                      <TableCell key={item.name + '_actions'}>
                        <div style={{ whiteSpace: 'nowrap' }}>
                          {this.props.actionButtons.map(
                            (actionButton, index) => {
                              // clone element so we can add a key element (needed for React map elements)
                              return React.cloneElement(actionButton(item), {
                                key: index,
                              });
                            }
                          )}
                        </div>
                      </TableCell>
                    </TableRow>
                  );
                })}
            </TableBody>
            <TableFooter>
              <TableRow>
                <TableCell>{primaryActionBtn}</TableCell>
                <TablePagination
                  count={data.length}
                  rowsPerPage={rowsPerPage}
                  page={page}
                  onChangePage={this.handleChangePage}
                  onChangeRowsPerPage={this.handleChangeRowsPerPage}
                />
              </TableRow>
            </TableFooter>
          </Table>
        </div>
      </Paper>
    );
  }
}

TableComponent.propTypes = {
  classes: PropTypes.object.isRequired,
  orderBy: PropTypes.string,
  columns: PropTypes.array,
  title: PropTypes.string,
  data: PropTypes.array,
  actionButtons: PropTypes.array.isRequired,
  onReload: PropTypes.func.isRequired,
  enableCellClick: PropTypes.bool.isRequired,
};

export default withStyles(styles)(TableComponent);
