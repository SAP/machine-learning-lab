import React from 'react';

import PropTypes from 'prop-types';
import styled from 'styled-components';

import FormControl from '@material-ui/core/FormControl';
import Input from '@material-ui/core/Input';
import MenuItem from '@material-ui/core/MenuItem';
import Select from '@material-ui/core/Select';

function ProjectSelector(props) {
  const { activeProject, className, projects, onProjectChange } = props;
  const projectElements = projects.map((project) => {
    return (
      <MenuItem value={JSON.stringify(project)} key={project.id}>
        {project.display_name}
      </MenuItem>
    );
  });

  // Material-UI throws a warning if the value is an empty object {}. So, it must either be one of the available options, e.g. {id: 'foo', name: 'foo'}, or ''.
  const selectId =
    Object.keys(activeProject).length === 0
      ? ''
      : JSON.stringify(activeProject);
  return (
    //   TODO: add div container?
    <FormControl className={`${className} formControl`}>
      <Select
        aria-label="projectselector"
        className={`${className} select`}
        value={selectId}
        input={<Input id="select-project" />}
        onChange={(e) => onProjectChange(JSON.parse(e.target.value))}
      >
        {projectElements}
      </Select>
    </FormControl>
  );
}

ProjectSelector.propTypes = {
  activeProject: PropTypes.instanceOf(Object),
  className: PropTypes.string,
  projects: PropTypes.arrayOf(Object),
  onProjectChange: PropTypes.func.isRequired,
};

ProjectSelector.defaultProps = {
  activeProject: {},
  className: '',
  projects: [],
};

const StyledProjectSelector = styled(ProjectSelector)`
  &.formControl {
    min-width: 120px;
    margin: ${(props) => props.theme.spacing(1)};
    margin-right: 24px;
  }
  &.select {
    color: white;
  }
`;

export default StyledProjectSelector;
