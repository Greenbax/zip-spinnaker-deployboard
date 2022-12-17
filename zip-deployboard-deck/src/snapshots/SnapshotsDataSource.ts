import type { Application, IDataSourceConfig } from '@spinnaker/core';
import { REST } from '@spinnaker/core';

export interface SnapshotType {
  branch: string;
  buildNumber: number;
  dockerImage: string;
  status: string;
  ttl: number;
  commits: CommitType[];
}

export interface CommitType {
  author: string;
  sha: string;
  message: string;
  ts: string;
}

interface SnapshotAPI {
  builds: Array<{
    branch: string;
    buildNumber: number;
    dockerImage: string;
    status: string;
    ttl: number;
    commits: Array<{
      author: string;
      sha: string;
      message: string;
      ts: string;
    }>;
  }>;
}

export const SnapshotsDataSource: IDataSourceConfig<SnapshotType[]> = {
  key: 'snapshots',
  label: 'Snapshots',
  autoActivate: true,
  activeState: '**.snapshots.**',
  visible: true,
  sref: '.snapshots',
  defaultData: [],
  description: 'Snapshot View',
  iconName: 'build',
  loader: (application: Application) => SnapshotsReader.getBuilds('prod'),
  onLoad: (application: Application, data: any) => Promise.resolve(data),
  lazy: true,
};

const transformSnapshot = (resp: SnapshotAPI): SnapshotType[] => {
  return resp.builds.map((val) => ({
    branch: val.branch,
    buildNumber: val.buildNumber,
    dockerImage: val.dockerImage,
    status: val.status,
    ttl: val.ttl,
    commits: transformCommits(val.commits),
  }));
};

const transformCommits = (commits: SnapshotAPI['builds'][0]['commits']): CommitType[] => {
  return commits.map((val): CommitType => ({ author: val.author, sha: val.sha, message: val.message, ts: val.ts }));
};

export class SnapshotsReader {
  public static get MAX_LINES(): number {
    return 4095;
  }

  public static getBuilds(branch: string, query?: string, lastSortKeySeen?: string): PromiseLike<SnapshotType[]> {
    const searchQuery: any = { branch };
    if (query) {
      searchQuery['query'] = query;
    }
    if (lastSortKeySeen) {
      searchQuery['lastSortKeySeen'] = lastSortKeySeen;
    }
    return REST('/extensions/snapshots')
      .query(searchQuery)
      .get()
      .then((builds) => {
        return transformSnapshot(builds);
      });
  }
}
