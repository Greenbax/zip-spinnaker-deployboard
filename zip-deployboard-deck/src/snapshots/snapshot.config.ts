export interface SnapshotConfigType {
  gitBranch: string;
  label: string;
  pipeline: string;
}

export const SNAPSHOT_CONFIGS: SnapshotConfigType[] = [
  {
    gitBranch: 'master',
    label: 'Staging',
    pipeline: 'Staging',
  },
  {
    gitBranch: 'production-release-branch-test',
    label: 'Next',
    pipeline: 'Next',
  },
  {
    gitBranch: 'qa',
    label: 'QA',
    pipeline: 'QA',
  },
  {
    gitBranch: 'prod',
    label: 'Production',
    pipeline: 'Website deployment',
  },
];
