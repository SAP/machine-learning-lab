import React, { useState } from 'react';

import { useTranslation } from 'react-i18next';
import styled from 'styled-components';

// styled-components uses Tagged Templates syntax (https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Template_literals ; https://styled-components.com/docs/basics)
const StyledButton = styled.button`
  margin-top: 12px;
  color: ${(props) => (props.isEnabled ? 'white' : 'black')};
  background-color: ${(props) => (props.isEnabled ? 'blue' : 'orange')};
`;

function Button() {
  const { t, i18n } = useTranslation();
  const [isEnabled, setEnabled] = useState();

  const click = () => {
    if (isEnabled) {
      i18n.changeLanguage('en');
    } else {
      i18n.changeLanguage('de');
    }
    setEnabled(!isEnabled);
  };
  const language = isEnabled ? 'Englisch' : 'German';
  return (
    <div>
      <StyledButton type="button" onClick={click} isEnabled={isEnabled}>
        {t('change-language', { language })}
      </StyledButton>
    </div>
  );
}

export default Button;
