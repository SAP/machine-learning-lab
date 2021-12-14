import React, { useState } from 'react';

import PropTypes from 'prop-types';

const AppDialogContext = React.createContext(null);
export const useShowAppDialog = () => React.useContext(AppDialogContext);

function AppDialogServiceProvider({ children }) {
  const [appDialogChildren, setAppDialogChildren] = useState(null);

  const showAppDialog = (component, props) => {
    setAppDialogChildren({ Component: component, props });
  };

  const hide = () => setAppDialogChildren(null);

  return (
    <>
      <AppDialogContext.Provider value={showAppDialog}>
        {children}
        {Boolean(appDialogChildren) && (
          <appDialogChildren.Component
            onClose={hide}
            {...appDialogChildren.props} // eslint-disable-line react/jsx-props-no-spreading
          />
        )}
      </AppDialogContext.Provider>
    </>
  );
}

AppDialogServiceProvider.propTypes = {
  children: PropTypes.node.isRequired,
};

export default AppDialogServiceProvider;
