import React from 'react';

import PropTypes from 'prop-types';
import styled from 'styled-components';

import Avatar from '@material-ui/core/Avatar';
import Button from '@material-ui/core/Button';
import Card from '@material-ui/core/Card';
import CardActions from '@material-ui/core/CardActions';
import CardContent from '@material-ui/core/CardContent';
import CardHeader from '@material-ui/core/CardHeader';
import Grid from '@material-ui/core/Grid';
import Typography from '@material-ui/core/Typography';

function UserCard(props) {
  const { className, onDelete, user, isHighlighted } = props;

  const createdAt = user.created_at ? user.created_at.toLocaleString() : '';

  const subHeader = user.username ? (
    <>
      <div className={`${className} cardTitle_projectId`} title="Project Id">
        {user.id}
      </div>
      <div>{createdAt}</div>
    </>
  ) : (
    createdAt
  );

  return (
    <>
      <Grid item>
        <Card className={`${className} card`}>
          <CardHeader
            avatar={
              <Avatar
                className={isHighlighted ? `${className} avatar_highlight` : ''}
                alt={user.username}
              />
            }
            title={user.username || user.id}
            subheader={subHeader}
          />
          <CardContent className={`${className} cardContent`}>
            <Typography>{user.email}</Typography>
          </CardContent>
          <CardActions>
            <Button onClick={() => onDelete(user)}>DELETE</Button>
          </CardActions>
        </Card>
      </Grid>
    </>
  );
}

UserCard.propTypes = {
  className: PropTypes.string,
  isHighlighted: PropTypes.bool,
  onDelete: PropTypes.func.isRequired,
  user: PropTypes.instanceOf(Object).isRequired,
};

UserCard.defaultProps = {
  className: '',
  isHighlighted: false,
};

const StyledUserCard = styled(UserCard)`
  &.card {
    display: flex;
    flex-direction: column;
    height: 100%;
  }

  &.cardContent {
    flex: 1;
    max-width: 400px;
    max-height: 100px;
    overflow: hide;
  }

  &.cardTitle_projectId {
    font-size: 0.75rem;
  }

  &.avatar_highlight {
    border: ${(props) => props.theme.palette.primary.main} 3px dotted;
  }
`;

export default StyledUserCard;
