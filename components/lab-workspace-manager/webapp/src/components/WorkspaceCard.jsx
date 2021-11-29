import React from 'react';

import PropTypes from 'prop-types';
import styled from 'styled-components';

import Button from '@material-ui/core/Button';
import Card from '@material-ui/core/Card';
import CardActions from '@material-ui/core/CardActions';
import CardContent from '@material-ui/core/CardContent';
import CardHeader from '@material-ui/core/CardHeader';
import Grid from '@material-ui/core/Grid';
import Typography from '@material-ui/core/Typography';

function WorkspaceCard(props) {
  const {
    className,
    workspaceService,
    onAccessClick,
    onModifyClick,
    onDeleteClick,
  } = props;

  let title = workspaceService.display_name || workspaceService.id;
  // Remove ws- prefix
  if (title.startsWith('ws-')) {
    title = title.substr(3);
  }

  return (
    <>
      <Grid item>
        <Card className={`${className} card`}>
          <CardHeader title={title} />
          <CardContent className={`${className} cardContent`}>
            {workspaceService.description && (
              <Typography>
                Description: {workspaceService.description}
              </Typography>
            )}
            {workspaceService.container_image && (
              <Typography>Image: {workspaceService.container_image}</Typography>
            )}
            {workspaceService.compute.max_cpus && (
              <Typography>CPUs: {workspaceService.compute.max_cpus}</Typography>
            )}
            {workspaceService.compute.max_memory && (
              <Typography>
                Memory: {workspaceService.compute.max_memory} GB
              </Typography>
            )}
            {workspaceService.status && (
              <Typography>Status: {workspaceService.status}</Typography>
            )}
          </CardContent>
          <CardActions>
            <Button onClick={() => onAccessClick(workspaceService)}>
              ACCESS
            </Button>
            <Button onClick={() => onModifyClick(workspaceService)}>
              MODIFY
            </Button>
            <Button onClick={() => onDeleteClick(workspaceService)}>
              DELETE
            </Button>
          </CardActions>
        </Card>
      </Grid>
    </>
  );
}
WorkspaceCard.propTypes = {
  className: PropTypes.string,
  workspaceService: PropTypes.instanceOf(Object).isRequired,
  onAccessClick: PropTypes.func.isRequired,
  onModifyClick: PropTypes.func.isRequired,
  onDeleteClick: PropTypes.func.isRequired,
};

WorkspaceCard.defaultProps = {
  className: '',
};

const StyledWorkspaceCard = styled(WorkspaceCard)`
  &.card {
    display: flex;
    flex-direction: column;
    height: 100%;
  }

  &.cardContent {
    overflow: hidden;
  }
`;

export default StyledWorkspaceCard;
