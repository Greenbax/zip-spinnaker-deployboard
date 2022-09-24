import type { IStageTypeConfig } from '@spinnaker/core';
import { DynamoStatusStageConfig, validate } from './DynamoStatusStageConfig';

/*
  Define Spinnaker Stages with IStageTypeConfig.
  Required options: https://github.com/spinnaker/deck/master/app/scripts/modules/core/src/domain/IStageTypeConfig.ts
  - label -> The name of the Stage
  - description -> Long form that describes what the Stage actually does
  - key -> A unique name for the Stage in the UI; ties to Orca backend
  - component -> The rendered React component
  - validateFn -> A validation function for the stage config form.
 */
export const dynamoStatusStage: IStageTypeConfig = {
  key: 'dynamoStatus',
  label: `Upload deploy status`,
  description: 'Stage that uploads the current status of the deploy.',
  component: DynamoStatusStageConfig, // stage config
  validateFn: validate,
};
