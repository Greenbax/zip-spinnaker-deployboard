import React from 'react';
import { Button } from 'react-bootstrap';

import { SortToggle, timestamp } from '@spinnaker/core';
import type { SnapshotType } from './SnapshotsDataSource';

export interface SnapshotsTableProps {
  commits: SnapshotType[];
  currentSort: string;
  toggleSort: (column: string) => void;
  toggleDeploy: (sha: string) => void;
}

export const SnapshotsTable = ({ commits, currentSort, toggleSort }: SnapshotsTableProps) => (
  <table className="table table-hover">
    <thead>
      <tr>
        <th style={{ width: '15%' }}>Deployed</th>
        <th style={{ width: '15%' }}>
          <SortToggle currentSort={currentSort} onChange={toggleSort} label="Author" sortKey="author" />
        </th>
        <th style={{ width: '15%' }}>Message</th>
        <th style={{ width: '15%' }}>Sha</th>
        <th style={{ width: '15%' }}>
          <SortToggle currentSort={currentSort} onChange={toggleSort} label="Timestamp" sortKey="timestamp" />
        </th>
        <th style={{ width: '15%' }}>Actions</th>
      </tr>
    </thead>

    <tbody>
      {commits.map((commit) => {
        return (
          <tr className="clickable">
            <td>{commit.currentlyDeployed}</td>
            <td>{commit.author}</td>
            <td>{commit.message}</td>
            <td>{commit.sha}</td>
            <td>{commit.timestamp}</td>
            <td>
              <Button>Deploy</Button>
            </td>
          </tr>
        );
      })}
    </tbody>
  </table>
);
