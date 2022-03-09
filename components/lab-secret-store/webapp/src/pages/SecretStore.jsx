import React from 'react';

import MaterialTable from '@material-table/core';
import Stack from '@mui/material/Stack';

const COLUMNS = [
  {
    field: 'name',
    title: 'Name',
    numeric: false,
    align: 'left',
  },
  {
    field: 'metadata',
    title: 'Metadata',
    numeric: false,
    align: 'left',
  },
];

const PAGE_SIZES = [5, 10, 15, 30, 50, 75, 100];

function SecretStore() {
  const data = [
    {
      name: 'Secret 1',
      metadata: 'test',
    },
    {
      name: 'Secret 2',
      metadata: '123',
    },
  ];
  return (
    <Stack
      sx={{
        height: '100%',
        ml: 3,
        mr: 3,
        mt: 6,
      }}
    >
      <MaterialTable
        title="Secrets"
        columns={COLUMNS}
        data={data}
        options={{
          filtering: false,
          columnsButton: false,
          grouping: false,
          pageSize: 5,
          pageSizeOptions: PAGE_SIZES,
          actionsColumnIndex: -1,
          headerStyle: {
            fontSize: '0.75rem',
            fontWeight: 500,
            fontFamily: 'Roboto',
          },
          rowStyle: {
            fontSize: '0.75rem',
            fontFamily: 'Roboto',
          },
        }}
        localization={{ header: { actions: '' } }} // disable localization header name
        actions={[
          {
            icon: 'autorenew',
            isFreeAction: true,
            onClick: () => {},
            tooltip: 'reload',
          },
          {
            icon: 'login',
            iconProps: { className: `` },
            onClick: (event, rowData) => {
              console.log(event, rowData);
            },
            tooltip: 'test',
          },
        ]}
      />
    </Stack>
  );
}

export default SecretStore;
