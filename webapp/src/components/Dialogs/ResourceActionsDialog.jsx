import React from 'react';

import PropTypes from 'prop-types';

import Button from '@material-ui/core/Button';
import Dialog from '@material-ui/core/Dialog';
import DialogActions from '@material-ui/core/DialogActions';
import DialogContent from '@material-ui/core/DialogContent';
import DialogTitle from '@material-ui/core/DialogTitle';

function ResourceActionsDialog(props) {
  const { resource, resourceActions, onClose, onExecuteAction, title } = props;

  const executeAction = async (resourceAction) => {
    onExecuteAction(resource, resourceAction);
  };

  const actionElements = resourceActions.map((resourceAction) => {
    return (
      <div>
        <Button
          key={resourceAction.action_id}
          onClick={() => executeAction(resourceAction)}
        >
          {resourceAction.display_name}
        </Button>
      </div>
    );
  });

  return (
    <Dialog open>
      <DialogTitle>{title}</DialogTitle>
      <DialogContent>{actionElements}</DialogContent>
      <DialogActions>
        <Button onClick={onClose} color="primary">
          CLOSE
        </Button>
      </DialogActions>
    </Dialog>
  );
}

ResourceActionsDialog.propTypes = {
  onClose: PropTypes.func.isRequired,
  onExecuteAction: PropTypes.func.isRequired,
  resource: PropTypes.instanceOf(Object).isRequired,
  resourceActions: PropTypes.instanceOf(Array).isRequired,
  title: PropTypes.string,
};

ResourceActionsDialog.defaultProps = {
  title: '',
};

export default ResourceActionsDialog;
