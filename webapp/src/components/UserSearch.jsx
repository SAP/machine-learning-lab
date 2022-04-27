import React from 'react';

import PropTypes from 'prop-types';
import styled from 'styled-components';

import Autocomplete, {
  createFilterOptions,
} from '@material-ui/lab/Autocomplete';
import TextField from '@material-ui/core/TextField';

function UserSearch(props) {
  const { className, userList, onUserSelect, selected } = props;

  const filterOptions = createFilterOptions({
    matchFrom: 'any',
    stringify: (option) => option.username + option.email + option.id,
  });

  return (
    <div style={{ width: 300 }} className={`${className} autocomplete`}>
      <Autocomplete
        id="user-search"
        onChange={(event, newValue) => onUserSelect(newValue)}
        freeSolo
        autoHighlight
        options={userList}
        value={selected}
        getOptionLabel={(option) => (option ? option.username : '')}
        filterOptions={filterOptions}
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
  // eslint-disable-next-line react/forbid-prop-types
  selected: PropTypes.object,
};

UserSearch.defaultProps = {
  className: '',
  userList: [],
  selected: {},
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
