import React from 'react';

import { Route, Routes } from 'react-router-dom';

import Box from '@mui/material/Box';

import SecretStore from './SecretStore';

function App() {
  return (
    <Box sx={{ height: '100vh' }}>
      <Routes>
        <Route path="*" element={<SecretStore />} />
      </Routes>
      <div id="snackbar-container" />
    </Box>
  );
}

export default App;
