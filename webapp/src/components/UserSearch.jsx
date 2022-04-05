import React from 'react';

import PropTypes from 'prop-types';
import styled from 'styled-components';

import Autocomplete from '@material-ui/lab/Autocomplete';
import TextField from '@material-ui/core/TextField';

function UserSearch(props) {
  const { className, userList, onUserSelect } = props;

  return (
    <div style={{ width: 300 }} className={`${className} autocomplete`}>
      <Autocomplete
        id="user-search"
        onChange={(event, newValue) => onUserSelect(newValue)}
        freeSolo
        options={userList.map((user) => user.username)}
        renderInput={(params) => (
          <TextField
            // eslint-disable-next-line react/jsx-props-no-spreading
            {...params}
            label="Search user"
            margin="normal"
            variant="outlined"
          />
        )}
      />
    </div>
  );
}

UserSearch.propTypes = {
  className: PropTypes.string,
  onUserSelect: PropTypes.func.isRequired,
  // eslint-disable-next-line react/forbid-prop-types
  userList: PropTypes.array,
};

UserSearch.defaultProps = {
  className: '',
  userList: [],
};

const StyledUserSearch = styled(UserSearch)`
  &.button {
    margin: 8px 0px;
  }

  &.widgetProjectsCount {
    flex: 0.3;
  }
`;

export default StyledUserSearch;
