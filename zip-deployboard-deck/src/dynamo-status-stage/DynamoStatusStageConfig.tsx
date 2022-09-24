import React from 'react';

import { CheckboxInput, FormikFormField, FormikStageConfig, FormValidator, TextInput } from '@spinnaker/core';
import type { IStage, IStageConfigProps } from '@spinnaker/core';

/*
  IStageConfigProps defines properties passed to all Spinnaker Stages.
  See IStageConfigProps.ts (https://github.com/spinnaker/deck/blob/master/app/scripts/modules/core/src/pipeline/config/stages/common/IStageConfigProps.ts) for a complete list of properties.
  Pass a JSON object to the `updateStageField` method to add the `maxWaitTime` to the Stage.
  This method returns JSX (https://reactjs.org/docs/introducing-jsx.html) that gets displayed in the Spinnaker UI.
 */
export function DynamoStatusStageConfig(props: IStageConfigProps) {
  return (
    <div>
      <FormikStageConfig
        {...props}
        validate={validate}
        onChange={props.updateStage}
        render={(props) => (
          <>
            <FormikFormField
              name="branch"
              label="Currently deploying branch"
              input={(props) => <TextInput {...props} />}
            />
            <FormikFormField
              name="success"
              label="Upload successful status"
              input={(props) => <CheckboxInput {...props} />}
            />
            <FormikFormField
              name="image"
              label="Expression for currently deploying image"
              input={(props) => <TextInput {...props} defaultValue="${trigger['properties']['IMAGE_TAG']}" />}
              spelAware
            />
          </>
        )}
      />
    </div>
  );
}

export function validate(stageConfig: IStage) {
  const validator = new FormValidator(stageConfig);

  validator.field('branch').required();

  return validator.validateForm();
}
