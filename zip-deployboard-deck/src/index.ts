import type { ApplicationStateProvider, IDeckPlugin } from '@spinnaker/core';
import { ApplicationDataSourceRegistry } from '@spinnaker/core';
import { dynamoStatusStage } from './dynamo-status-stage/DynamoStatusStage';

import { Snapshots, SnapshotsDataSource } from './snapshots';

export const plugin: IDeckPlugin = {
  stages: [dynamoStatusStage],
  initialize: () => {
    console.log('initializing');
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
