import type { Application, IDataSourceConfig } from '@spinnaker/core';
import { REST } from '@spinnaker/core';

export interface SnapshotType {
  sha: string;
  message: string;
  author: string;
  timestamp: string;
  branch: string;
  currentlyDeployed?: string;
}

export const SnapshotsDataSource: IDataSourceConfig<SnapshotType[]> = {
  key: 'snapshots',
  label: 'Snapshots',
  autoActivate: true,
  activeState: '**.snapshots.**',
  visible: true,
  sref: '.snapshots',
  defaultData: [
    {
      author: 'test',
      sha: 'test',
      message: 'test',
      timestamp: 'test',
      branch: 'test',
      currentlyDeployed: 'test',
    },
    {
      author: 'test',
      sha: 'test',
      message: 'test',
      timestamp: 'test',
      branch: 'test',
      currentlyDeployed: 'test',
    },
    {
      author: 'test',
      sha: 'test',
      message: 'test',
      timestamp: 'test',
      branch: 'test',
      currentlyDeployed: 'test',
    },
  ],
  description: 'Snapshot View',
  iconName: 'build',
  loader: (application: Application) => SnapshotsReader.getBuilds(),
  onLoad: (application: Application, data: any) => Promise.resolve(data),
  lazy: true,
};

const transformSnapshot = (build) => {
  return;
};

export class SnapshotsReader {
  public static get MAX_LINES(): number {
    return 4095;
  }

  public static getBuilds(): PromiseLike<SnapshotType[]> {
    return REST('snapshots/builds')
      .get()
      .then((builds) => builds.map((build: any) => transformSnapshot(build)));
  }
}
