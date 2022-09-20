import type { ApplicationStateProvider, IDeckPlugin } from '@spinnaker/core';
import { ApplicationDataSourceRegistry } from '@spinnaker/core';

import { Snapshots, SnapshotsDataSource } from './snapshots';

export const plugin: IDeckPlugin = {
  initialize: () => {
    const injector = (window as any).spinnaker.$injector;
    const applicationState: ApplicationStateProvider = injector.get('applicationState');
    applicationState.addChildState({
      name: 'snapshots',
      url: '/snapshots',
      views: {
        insight: {
          component: Snapshots,
          $type: 'react',
        },
      },
    });

    ApplicationDataSourceRegistry.registerDataSource(SnapshotsDataSource);
  },
};
