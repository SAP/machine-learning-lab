import React, { useEffect, useRef } from 'react';

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

import * as Jdenticon from 'jdenticon';

function ProjectCard(props) {
  const {
    className,
    onClickManageMembers,
    onDeleteProject,
    onSelect,
    project,
    isHighlighted,
  } = props;
  const svgRef = useRef(null);
  useEffect(() => {
    Jdenticon.update(svgRef.current);
  }, [svgRef]);

  const createdAt = project.created_at
    ? project.created_at.toLocaleString()
    : '';

  const subHeader = project.display_name ? (
    <>
      <div className={`${className} cardTitle_projectId`} title="Project Id">
        {project.id}
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
                alt={project.display_name}
              >
                <svg
                  ref={svgRef}
                  width="60"
                  height="60"
                  style={{ backgroundColor: 'white' }}
                  data-jdenticon-value={project.display_name}
                />
              </Avatar>
            }
            title={project.display_name || project.id}
            subheader={subHeader}
          />
          <CardContent className={`${className} cardContent`}>
            <Typography>{project.description}</Typography>
          </CardContent>
          <CardActions>
            <Button onClick={() => onSelect(project)}>SELECT</Button>
            {!project.technical_project
              ? [
                  <Button onClick={() => onClickManageMembers(project)}>
                    MEMBERS
                  </Button>,
                  <Button onClick={() => onDeleteProject(project)}>
                    DELETE
                  </Button>,
                ]
              : null}
          </CardActions>
        </Card>
      </Grid>
    </>
  );
}

ProjectCard.propTypes = {
  className: PropTypes.string,
  isHighlighted: PropTypes.bool,
  onClickManageMembers: PropTypes.func.isRequired,
  onDeleteProject: PropTypes.func.isRequired,
  onSelect: PropTypes.func.isRequired,
  project: PropTypes.instanceOf(Object).isRequired,
};

ProjectCard.defaultProps = {
  className: '',
  isHighlighted: false,
};

const StyledProjectCard = styled(ProjectCard)`
  &.card {
    display: flex;
    flex-direction: column;
    height: 100%;
    width: 400px;
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

export default StyledProjectCard;
