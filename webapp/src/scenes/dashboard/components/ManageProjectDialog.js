import React from 'react';
import PropTypes from 'prop-types';
import Select from 'react-select';

// material-ui components
import { withStyles } from '@material-ui/core/styles';
import Button from '@material-ui/core/Button';
import Input from '@material-ui/core/Input';
import List from '@material-ui/core/List';
import DialogActions from '@material-ui/core/DialogActions';

import {
  authorizationApi,
  getDefaultApiCallback,
  toastErrorMessage,
  toastSuccess,
} from '../../../services/client/ml-lab-api';
import { Typography } from '@material-ui/core';

import ListItem from '@material-ui/core/ListItem';
import ListItemSecondaryAction from '@material-ui/core/ListItemSecondaryAction';
import ListItemText from '@material-ui/core/ListItemText';
import AccountIcon from '@material-ui/icons/AccountCircle';
import HighlightOff from '@material-ui/icons/HighlightOff';

import IconButton from '@material-ui/core/IconButton';

const styles = (theme) => {
  const DIALOG_PADDING = 8;
  return {
    root: {
      overflow: 'initial',
      width: 550,
    },
    userRow: {
      //marginBottom: 12
      paddingLeft: 0,
    },
    memberText: {
      marginLeft: DIALOG_PADDING,
      fontWeight: 'initial',
      display: 'inline',
      paddingLeft: '0px !important',
    },
    selectedUserRow: {
      backgroundColor: 'rgba(63, 81, 181, 0.12)',
    },
    userSearch: {
      marginLeft: DIALOG_PADDING,
    },
    userInput: {
      display: 'block',
      minWidth: '70%',
    },
    actionButton: {
      marginLeft: 6,
      width: 75,
      // marginTop: 12,
      padding: DIALOG_PADDING,
    },
    actionButtonLabel: {
      justifyContent: 'middle',
    },
  };
};

// const SearchField = props => {
// 	console.log("props serach field", props);
// 	let { className, inputComponent, value, inputProps } = props;
// 	return (
// 		<TextField
// 			id="search-textfield"
// 			label="Search a User"
// 			// multiline
// 			// className={classes.textField}
// 			name="searchField"
// 			// onChange={e => this.handleInputChange(e)}
// 			margin="dense"
// 			fullWidth
// 			className={className}
// 			inputComponent={inputComponent}
// 			value={value}
// 			inputProps={inputProps}
// 		/>
// 	);
// };

// https://zeit.co/blog/async-and-await
function sleep(time) {
  return new Promise((resolve) => setTimeout(resolve, time));
}
class ManageProjectDialog extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      isAddEnabled: false,
      isRemoveEnabled: false,
      memberToAdd: {}, // for adding to project
      selectedMember: '', // for removing from project
      userNameOptions: [], // used for suggesting the names. Format {value: "", label: ""}
      projectMembers: props.projectMembers, // list of names
      requestClose: props.requestClose,
    };
  }

  componentDidMount() {
    let { projectMembers } = this.state;
    authorizationApi.getUsers(
      {},
      getDefaultApiCallback(({ result }) => {
        let userNames = result.data
          // only show users in search suggestion who are not already part of the project
          .filter((item) => projectMembers.indexOf(item.name) === -1)
          // bring results into the right form for react-select's suggestion
          .map((item) => {
            return { value: item.name, label: item.name };
          });
        this.setState({
          userNameOptions: userNames,
        });
      })
    );
  }

  /**
   * User selected in search
   */
  onUserSearch = function (selectedUser) {
    let isAddEnabled = true;
    if (selectedUser.length === 0) {
      isAddEnabled = false;
    }
    this.setState({
      isAddEnabled: isAddEnabled,
      isRemoveEnabled: false,
      memberToAdd: selectedUser,
      selectedMember: '',
    });
  }.bind(this);

  /**
   * Select user row
   */
  onSelectUser = function (member) {
    let isRemoveEnabled = true;
    if (member === this.state.selectedMember) {
      member = '';
      isRemoveEnabled = false;
    }
    this.setState({
      isAddEnabled: false,
      isRemoveEnabled: isRemoveEnabled,
      memberToAdd: { value: '', label: '' },
      selectedMember: member,
    });
  }.bind(this);

  addUser = function (project) {
    const user = this.state.memberToAdd.value;
    authorizationApi.addUserToProject(
      user,
      project,
      {},
      getDefaultApiCallback(
        () => {
          toastSuccess('Added ' + user + ' to project ' + project);
          this.setState({
            // add user to project members on UI side
            projectMembers: [...this.state.projectMembers, user],
            isAddEnabled: false,
            memberToAdd: {},

            // don't show added user in search suggestions anymore
            userNameOptions: this.state.userNameOptions.filter(
              (option) => option.value !== user
            ),
          });
        },
        ({ error }) => toastErrorMessage('Adding user failed: ', error)
      )
    );
  };

  deleteUser = function (user, project) {
    authorizationApi.removeUserFromProject(
      user,
      project,
      {},
      getDefaultApiCallback(
        () => {
          toastSuccess('Removed ' + user + ' from project ' + project);
          // remove user from project members list
          let modifiedProjectMembers = this.state.projectMembers.filter(
            (member) => member !== user
          );
          this.setState({
            // update project members on UI side
            projectMembers: modifiedProjectMembers,
            isRemoveEnabled: false,
            selectedMember: { value: '', label: '' },

            // adding the user to the search suggestions
            userNameOptions: [
              ...this.state.userNameOptions,
              { value: user, label: user },
            ],
          });
        },
        ({ error }) => toastErrorMessage('Removing user failed: ', error)
      )
    );
  };
  onActionButtonClick = function () {
    const { project } = this.props;
    if (this.state.isAddEnabled) {
      this.addUser(project);
    } else if (this.state.isRemoveEnabled) {
      this.deleteUser(project);
    }
  };

  render() {
    const { classes } = this.props;

    return (
      <div className={classes.root}>
        <div style={{ display: 'flex', padding: '24px' }}>
          <div style={{ width: '50%', display: 'inline-block', margin: '4px' }}>
            <Typography
              variant="subtitle1"
              style={{ color: 'grey', fontSize: 13 }}
            >
              Members Of Your Project:
            </Typography>

            <List
              className={classes.list}
              style={{
                backgroundColor: '#b5b5b514',
                borderRadius: '6px',
                padding: '8px',
                margin: '8px',
              }}
            >
              {this.state.projectMembers.map((member, index) => {
                return (
                  <ListItem key={index}>
                    {/* <ListItemAvatar>
											<Avatar> */}
                    <AccountIcon style={{ color: 'grey' }} />
                    {/* </Avatar>
										</ListItemAvatar> */}
                    <ListItemText
                      primary={member}
                      // secondary={secondary ? 'Secondary text' : null}
                    />
                    <ListItemSecondaryAction>
                      <IconButton
                        aria-label="Delete"
                        onClick={(e) => {
                          const { project } = this.props;
                          // console.log('member', member);
                          // console.log('project', project);
                          this.onSelectUser(member);
                          this.deleteUser(member, project);
                        }}
                      >
                        <HighlightOff style={{ color: 'E91E63' }} />
                      </IconButton>
                    </ListItemSecondaryAction>
                  </ListItem>
                );
              })}
            </List>
          </div>

          <div
            style={{
              width: '50%',
              display: 'inline-block',
              paddingLeft: '8px',
            }}
          >
            <Typography
              variant="subtitle1"
              style={{ color: 'grey', fontSize: 13 }}
            >
              Add User:
            </Typography>
            <div style={{ display: 'flex', padding: '4px' }}>
              <Input
                className={classes.userInput}
                inputComponent={SelectWrapped}
                value={this.state.memberToAdd.value}
                placeholder="Search users"
                inputProps={{
                  classes,
                  onChange: this.onUserSearch,
                  options: this.state.userNameOptions,
                  memberToAdd: this.state.memberToAdd,
                }}
              />
              {this.state.isAddEnabled ? (
                <Button
                  classes={{
                    root: classes.actionButton,
                    label: classes.actionButtonLabel,
                  }}
                  color="secondary"
                  onClick={(e) => this.onActionButtonClick(e)}
                >
                  ADD
                </Button>
              ) : (
                <Button
                  classes={{
                    root: classes.actionButton,
                    label: classes.actionButtonLabel,
                  }}
                  color="secondary"
                  disabled
                  onClick={(e) => this.onActionButtonClick(e)}
                >
                  ADD
                </Button>
              )}
            </div>
          </div>
        </div>

        <DialogActions>
          <Button
            size="small"
            style={{ width: '100px' }}
            color="primary"
            onClick={this.state.requestClose}
          >
            {' '}
            Cancel
          </Button>
          <Button
            size="small"
            style={{ width: '100px' }}
            color="primary"
            onClick={(e) => {
              this.onActionButtonClick(e);
              sleep(1500).then(() => {
                // Do something after the sleep!
                this.state.requestClose();
              });
            }}
          >
            {' '}
            OK
          </Button>
        </DialogActions>
      </div>
    );
  }
}

ManageProjectDialog.propTypes = {
  project: PropTypes.string.isRequired,
  projectMembers: PropTypes.array,
  requestClose: PropTypes.func.isRequired,
};

class SelectWrapped extends React.Component {
  customStyles = {
    control: (styles) => ({
      ...styles,
      border: 0,
      padding: 0,
      margin: 0,
      background: 'transparent',
      boxShadow: 'none',
    }),
    indicatorsContainer: (styles) => ({
      ...styles,
      display: 'none',
    }),
  };

  render() {
    const { inputRef, options, memberToAdd } = this.props;
    return (
      <Select
        ref={inputRef}
        placeholder="Find users to add..."
        value={memberToAdd}
        onChange={this.props.onChange}
        options={options}
        styles={this.customStyles}
      />
    );
  }
}

SelectWrapped.propTypes = {
  onChange: PropTypes.func.isRequired,
  memberToAdd: PropTypes.object,
  options: PropTypes.array.isRequired,
  classes: PropTypes.object.isRequired, // from withStyles
};

export default withStyles(styles)(ManageProjectDialog);
