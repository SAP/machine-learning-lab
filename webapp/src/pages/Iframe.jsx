import React from 'react';

import PropTypes from 'prop-types';
import ReactIframe from 'react-iframe';
import styled from 'styled-components';

// import CircularProgress from '@material-ui/core/CircularProgress';

import GlobalStateContainer from '../app/store';

function Iframe(props) {
  const { className, url, projectSpecific } = props;
  const { activeProject } = GlobalStateContainer.useContainer();
  // const [isLoading, setIsLoading] = useState(true);

  let iframeUrl = url || '';
  if (projectSpecific) {
    // use '?' if url does not contain a query parameter yet, '&' otherwise
    const delimiter = url.indexOf('?') > -1 ? '&' : '?';
    iframeUrl = `${url}${delimiter}project=${activeProject.id}`;
  }

  // const handleOnLoad = () => {
  //   setIsLoading(false);
  // };

  // const ProgressComponent = isLoading ? (
  //   <CircularProgress className={`${className} progress`} />
  // ) : null;

  return (
    <div className={`${className} root`}>
      {/* {ProgressComponent} */}
      <ReactIframe
        url={iframeUrl}
        allowFullScreen
        className={`${className} iframe`}
        // onLoad={handleOnLoad}
      />
    </div>
  );
}

Iframe.propTypes = {
  className: PropTypes.string,
  url: PropTypes.string.isRequired,
  projectSpecific: PropTypes.bool,
};

Iframe.defaultProps = {
  className: '',
  projectSpecific: false,
};

const StyledIframeComponent = styled(Iframe)`
  &.root {
    flex-grow: 1;
  }

  &.iframe {
    width: 100%;
    height: 100%;
    border: none;
  }

  /* Shows the progress bar as an overlay */
  &.progress {
    position: absolute;
    top: 80px;
  }
`;

export default StyledIframeComponent;
