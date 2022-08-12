import { createTheme } from '@mui/material';

export default createTheme({
  palette: {
    primary: {
      light: '#6573c3',
      main: '#3f51b5',
      dark: '#2c387e',
      contrastText: '#fff',
    },
    secondary: {
      light: '#5393ff',
      main: '#2979ff',
      dark: '#1c54b2',
      contrastText: '#000',
    },
    gray: '#646464',
  },
  overrides: {
    MuiTypography: {
      body1: {
        fontSize: '0.875rem',
      },
    },
  },
});
