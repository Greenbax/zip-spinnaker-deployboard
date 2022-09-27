import { ArrowDropDown, ArrowRight, Restore } from '@material-ui/icons';
import React from 'react';
import { Button } from 'react-bootstrap';
import { CheckmarkIcon, LoaderIcon } from 'react-hot-toast';

import type { CommitType, SnapshotType } from './SnapshotsDataSource';

export interface SnapshotsTableProps {
  builds: SnapshotType[];
  expandedBuilds: Set<number>;
  expandBuild: (buildNumber: number) => void;
  collapseBuild: (buildNumber: number) => void;
  toggleDeploy: (sha: string) => void;
}

const getCommitSummary = (commits: CommitType[]): string => {
  if (commits.length == 0) {
    return '';
  }
  if (commits.length == 1) {
    return commits[0].message;
  }
  return `${commits[0].message} + ${commits.length - 1} ${commits.length === 2 ? 'other' : 'others'}...`;
};

export const SnapshotsTable = ({
  builds,
  expandedBuilds,
  collapseBuild,
  expandBuild,
  toggleDeploy,
}: SnapshotsTableProps) => {
  const toggleBuild = (buildNumber: number) => {
    if (expandedBuilds.has(buildNumber)) {
      collapseBuild(buildNumber);
    } else {
      expandBuild(buildNumber);
    }
  };

  return builds.length ? (
    <table className="table table-hover">
      <thead>
        <tr>
          <th style={{ width: '5%' }}></th>
          <th style={{ width: '5%' }}>Status</th>
          <th style={{ width: '15%' }}>Jenkins Buid Number</th>
          <th style={{ width: '45%' }}>Commits</th>
          <th style={{ width: '25%' }}>Docker Image Tag</th>
          <th style={{ width: '5%' }}>Actions</th>
        </tr>
      </thead>

      <tbody>
        {builds.map((build) => {
          return (
            <>
              <tr className="clickable" onClick={() => toggleBuild(build.buildNumber)}>
                <td>
                  {expandedBuilds.has(build.buildNumber) ? (
                    <ArrowDropDown fontSize="large" />
                  ) : (
                    <ArrowRight fontSize="large" />
                  )}
                </td>
                <td>
                  <div style={{ display: 'flex', textAlign: 'center', alignItems: 'center' }}>
                    {build.status === 'DEPLOYING' ? (
                      <LoaderIcon />
                    ) : build.status === 'DEPLOYED' ? (
                      <CheckmarkIcon />
                    ) : build.status === 'LAST_DEPLOYED' ? (
                      <Restore fontSize="large" />
                    ) : (
                      <></>
                    )}
                  </div>
                </td>
                <td>{build.buildNumber}</td>
                <td>{getCommitSummary(build.commits)}</td>
                <td>{build.dockerImage}</td>
                <td>
                  <Button
                    onClick={(e) => {
                      e.stopPropagation();
                      toggleDeploy(build.dockerImage);
                    }}
                  >
                    Deploy
                  </Button>
                </td>
              </tr>
              {expandedBuilds.has(build.buildNumber) && (
                <tr>
                  <td colSpan={6}>
                    <table className="table">
                      <thead>
                        <tr>
                          <th style={{ width: '15%' }}>Author</th>
                          <th style={{ width: '60%' }}>Message</th>
                          <th style={{ width: '25%' }}>Sha</th>
                        </tr>
                        {build.commits.map((commit) => {
                          return (
                            <tr>
                              <td>{commit.author}</td>
                              <td>{commit.message}</td>
                              <td>
                                <a href={`https://github.com/Greenbax/evergreen/commit/${commit.sha}`}>{commit.sha}</a>
                              </td>
                            </tr>
                          );
                        })}
                      </thead>
                    </table>
                  </td>
                </tr>
              )}
            </>
          );
        })}
      </tbody>
    </table>
  ) : (
    <h2>No snapshots yet.</h2>
  );
};
