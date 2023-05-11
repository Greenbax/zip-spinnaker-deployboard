export interface SnapshotConfigType {
  gitBranch: string;
  label: string;
  pipeline: string;
}

export const NEXT_PREVIEW = 'Next-Preview';

export const SNAPSHOT_CONFIGS: Map<string, SnapshotConfigType> = new Map<string, SnapshotConfigType>([
  [
    'prod',
    {
      gitBranch: 'prod',
      label: 'Production',
      pipeline: 'Website deployment',
    },
  ],
  [
    'master',
    {
      gitBranch: 'master',
      label: 'Staging',
      pipeline: 'Staging',
    },
  ],
  [
    'test-build-branch',
    {
      gitBranch: 'test-build-branch',
      label: NEXT_PREVIEW,
      pipeline: NEXT_PREVIEW,
    },
  ],
  [
    'qa',
    {
      gitBranch: 'qa',
      label: 'QA',
      pipeline: 'QA',
    },
  ],
  [
    'czhen-test',
    {
      gitBranch: 'czhen-test',
      label: 'Test',
      pipeline: 'test-czhen',
    },
  ],
]);
