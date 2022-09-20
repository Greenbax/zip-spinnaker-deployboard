import { useCurrentStateAndParams, useRouter } from '@uirouter/react';
import React, { useState } from 'react';

import type { Application } from '@spinnaker/core';
import { useDataSource } from '@spinnaker/core';

import type { SnapshotType } from './SnapshotsDataSource';
import { SnapshotsTable } from './SnapshotsTable';

interface SnapshotsProps {
  app: Application;
}

export function Snapshots(props: SnapshotsProps) {
  const dataSource = props.app.getDataSource('snapshots');
  const { data, status, loaded } = useDataSource<SnapshotType[]>(dataSource);
  React.useEffect(() => {
    dataSource.activate();
  }, []);

  const router = useRouter();
  const { params, state } = useCurrentStateAndParams();
  const [deployCommit, setDeployCommit] = useState('');
  const [currentSort, setCurrentSort] = useState('Timestamp');

  return (
    <SnapshotsTable
      commits={data}
      toggleDeploy={setDeployCommit}
      currentSort={currentSort}
      toggleSort={setCurrentSort}
    />
  );
}
