import React, { useState, useEffect } from 'react';

import PropTypes from 'prop-types';

import { withStyles } from '@material-ui/core/styles';
import Button from '@material-ui/core/Button';
import Dialog from '@material-ui/core/Dialog';
import DialogActions from '@material-ui/core/DialogActions';
import Typography from '@material-ui/core/Typography';
import Icon from '@material-ui/core/Icon';
import IconButton from '@material-ui/core/IconButton';
import Tooltip from '@material-ui/core/Tooltip';

import * as GridFormatter from './grid-formatter';

const styles = () => ({
  columnDialog: {
    maxWidth: '82%',
    padding: '8px',
  },
  tableContent: {
    display: 'flex',
    flexDirection: 'row',
    margin: '8px',
    overflowX: 'scroll',
  },
  column: {
    display: 'flex',
    marginRight: '16px',
    flexDirection: 'column',
    alignItems: 'center',
  },
  headerCell: {
    color: 'rgba(0, 0, 0, 0.54)',
    marginBottom: '8px',
  },
  cell: {
    marginBottom: '4px',
    textAlign: 'center',
    width: '100%',
    whiteSpace: 'nowrap',
  },
  button: {
    width: '100%',
    border: 'none',
    color: 'white',
  },
  buttonValue: {
    fontSize: '0.95em',
  },
  horizontalDivider: {
    border: 'solid',
    margin: '0 auto',
    width: '98%',
    borderWidth: '0.5px',
    borderColor: 'rgb(227, 228, 227)',
  },
  actionButton: {
    fontSize: '0.85rem',
  },
});

const GRAY_COLOR = 'rgba(0, 0, 0, 0.20)';

/**
 * comparison for alphabetical order
 */
const alphabeticalComparator = (a, b) => {
  if (a[0] < b[0]) return -1;
  if (a[0] > b[0]) return 1;
  return 0;
};

/**
 * Button that indicates whether a column is visible or not
 * Each Button within the table is defined as a class in order to keep track of the active state
 *
 */
function ColumnButton(props) {
  const [active, setActive] = useState();

  // set active state when property is changed
  useEffect(() => setActive(props.active), [props.active]);

  const backgroundColor = active ? '#E91E63' : GRAY_COLOR;

  const buttonValue = GridFormatter.makeUpperCaseAndAddWhitespace(
    props.attribute
  );

  const onClick = () => {
    setActive(!active);
    props.onClick();
  };

  return (
    <Button
      variant="outlined"
      size="small"
      className={props.classes.button}
      style={{
        backgroundColor: backgroundColor,
      }}
      onClick={onClick}
    >
      <span className={props.classes.buttonValue}>{buttonValue}</span>
    </Button>
  );
}

function Column(props) {
  const capitalizedColumnName =
    props.columnName.charAt(0).toUpperCase() + props.columnName.slice(1);

  return (
    <div className={props.classes.column}>
      <Typography className={props.classes.headerCell}>
        {capitalizedColumnName}
      </Typography>
      {Object.values(props.column)
        .sort((a, b) => alphabeticalComparator(a.attribute, b.attribute))
        .map((columnEntry, index) => {
          // console.log("map");
          return (
            <div key={index} className={props.classes.cell}>
              <ColumnButton
                classes={props.classes}
                attribute={columnEntry.attribute}
                active={columnEntry.visible}
                onClick={() => {
                  props.onClick(columnEntry.attribute, !columnEntry.visible);
                }}
              />
            </div>
          );
        })}
    </div>
  );
}

// Don't re-render the columns when state in AttributeDialog changes as this is can be a heavy process and not needed!
const MemoizedColumn = React.memo(Column, (prevProps, nextProps) => true);

/**
 * A Dialog component that will show columns of the form of
 * Column1      | Column2      | ... | Column X
 * ------------------------------------------------
 * attribute1.1 | attribute2.1 | ... | attributeX.1
 * attribute1.2 |              | ... | attributeX.2
 * attribute1.3 |              | ... |
 */
function AttributeDialog(props) {
  const [isOpen, setOpen] = useState(false);
  const [columns, setColumns] = useState({});

  useEffect(() => setColumns(props.attributeColumns), [props.attributeColumns]);

  const onCancel = () => {
    setOpen(false);
  };

  const onOk = () => {
    setOpen(false);
    props.onAttributeActivatedChange(columns);
  };

  return (
    <div>
      <div id="customSelector">
        <Tooltip title="Columns" placement="bottom">
          <IconButton
            // size="small"
            color="default"
            onClick={() => setOpen(true)}
          >
            <Icon>visibility_off</Icon>
          </IconButton>
        </Tooltip>
      </div>

      <Dialog
        classes={{ paper: props.classes.columnDialog }}
        open={isOpen}
        onClose={onCancel}
      >
        <div className={props.classes.tableContent}>
          {Object.keys(props.attributeColumns).map((columnName, index) => {
            return (
              <MemoizedColumn
                key={index}
                columnName={columnName}
                column={props.attributeColumns[columnName]}
                classes={props.classes}
                onClick={(attributeName, visible) => {
                  let modifiedColumns = { ...props.attributeColumns };
                  modifiedColumns[columnName][attributeName].visible = visible;
                  setColumns(modifiedColumns);
                }}
              />
            );
          })}
        </div>
        <div className={props.classes.horizontalDivider} />
        <DialogActions>
          <Button size="small" color="primary" onClick={onCancel}>
            <Typography className={props.classes.actionButton}>
              Cancel
            </Typography>
          </Button>
          <Button size="small" color="primary">
            <Typography className={props.classes.actionButton} onClick={onOk}>
              OK
            </Typography>
          </Button>
        </DialogActions>
      </Dialog>
    </div>
  );
}

AttributeDialog.propTypes = {
  // dictionary in the form of:
  // { category1: {subCategory1: {name: subCategory1, visible: bool}, subCategory2: {}}, category2: ... }
  attributeColumns: PropTypes.object.isRequired,
  // callback that accepts a dictionary with the same structure as `attributeColumns`
  onAttributeActivatedChange: PropTypes.func.isRequired,
};

export default withStyles(styles)(AttributeDialog);
