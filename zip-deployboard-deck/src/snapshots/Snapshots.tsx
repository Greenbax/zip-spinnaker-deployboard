import { ArrowDropDown } from '@material-ui/icons';
import { UISref, useCurrentStateAndParams } from '@uirouter/react';
import { cloneDeep } from 'lodash';
import React, { useEffect, useState } from 'react';
import { Button, Dropdown, Form, Modal, ModalBody } from 'react-bootstrap';
import toast, { LoaderIcon } from 'react-hot-toast';
import InfiniteScroll from 'react-infinite-scroll-component';

import type { Application, IPipelineCommand } from '@spinnaker/core';
import { ModalClose, ReactInjector, TextInput, useData } from '@spinnaker/core';

import type { SnapshotType } from './SnapshotsDataSource';
import { SnapshotsReader } from './SnapshotsDataSource';
import { SnapshotsTable } from './SnapshotsTable';
import { NEXT_PREVIEW, SNAPSHOT_CONFIGS } from './snapshot.config';

import './Snapshots.less';

export const DOCKER_PREFIX = '242230929264.dkr.ecr.us-east-2.amazonaws.com/evergreen-server:';

interface SnapshotsProps {
  app: Application;
}

const getBuildsWrapped = (branch: string, query?: string, lastSortKeySeen?: string) => () =>
  SnapshotsReader.getBuilds(branch, query, lastSortKeySeen);

export function Snapshots(props: SnapshotsProps) {
  const { params } = useCurrentStateAndParams();
  const branch = params.branch;
  const [query, setQuery] = useState('');
  const [lastSortKeySeen, setLastSortKeySeen] = useState('');
  const [deployBuild, setDeployBuild] = useState('');
  const [deployPipeline, setDeployPipeline] = useState('prod');
  const [expandedBuilds, setExpandedBuilds] = useState(new Set<number>());

  const { result, status } = useData<SnapshotType[]>(
    getBuildsWrapped(branch, query || undefined, lastSortKeySeen || undefined),
    [],
    [query, branch, lastSortKeySeen],
  );
  const [builds, setBuilds] = useState<SnapshotType[]>([]);
  useEffect(() => {
    setBuilds(dedupeBuilds(builds.concat(result)));
  }, [result]);

  // Use a wrapped state setter so we can clear branch data cache.
  const setQueryWrapped = (query: string) => {
    setBuilds([]);
    setLastSortKeySeen('');
    setQuery(query);
  };

  const expandBuild = (buildNumber: number) => setExpandedBuilds(new Set<number>(expandedBuilds.add(buildNumber)));
  const collapseBuild = (buildNumber: number) => {
    const copy = new Set<number>(expandedBuilds);
    copy.delete(buildNumber);
    setExpandedBuilds(copy);
  };

  const dedupeBuilds = (builds: SnapshotType[]): SnapshotType[] => {
    const dedupedBuilds = builds.reduce((acc, val) => acc.set(val.buildNumber, val), new Map());
    return Array.from(dedupedBuilds.values()).sort((a, b) => b.buildNumber - a.buildNumber);
  };

  const startPipeline = (command: IPipelineCommand): PromiseLike<void> => {
    if (
      command.pipelineName === NEXT_PREVIEW &&
      (!command.parameters.name || (command.parameters.name && command.parameters.name.indexOf(' ') >= 0))
    ) {
      window.alert('Need a name without spaces for the environment');
      return;
    }
    const { executionService } = ReactInjector;
    const buildsCopy = cloneDeep(builds);
    buildsCopy.find((val) => val.dockerImage === deployBuild).status = 'DEPLOYING';
    setBuilds(buildsCopy);
    setDeployBuild('');
    return executionService
      .startAndMonitorPipeline(props.app, command.pipelineName, command.trigger)
      .then((monitor: any) => {
        toast.success(`Pipeline '${command.pipelineName}' started!`);
        return monitor.promise;
      });
  };

  return (
    <>
      <SnapshotDeployModal
        pipeline={SNAPSHOT_CONFIGS.get(deployPipeline).pipeline}
        setPipeline={setDeployPipeline}
        image={deployBuild}
        onClose={() => setDeployBuild('')}
        onSubmit={startPipeline}
      />
      <div style={{ width: '95%', overflow: 'hidden' }}>
        <InfiniteScroll
          hasMore={builds.length % 25 === 0}
          dataLength={builds.length}
          loader={<LoaderIcon />}
          height={'95vh'}
          next={() => {
            if (builds.length === 0) {
              return;
            }
            const lastBuild = builds[builds.length - 1];
            const lastCommit = lastBuild.commits[lastBuild.commits.length - 1];
            setLastSortKeySeen(
              `${lastBuild.ttl}#${lastBuild.buildNumber}#${lastCommit.ts}#${lastCommit.author}#${lastCommit.sha}`,
            );
          }}
        >
          <div style={{ display: 'flex', marginBottom: '20px' }}>
            <TextInput
              autoFocus
              onChange={(val) => setQueryWrapped(val.target.value)}
              value={query}
              placeholder="Author or commit message (note: search is case sensitive)..."
            />
            <div style={{ marginLeft: '20px' }}>
              <Dropdown>
                <Dropdown.Toggle>
                  <span style={{ display: 'flex', alignItems: 'center' }}>
                    {`Branch: ${branch}`}
                    <ArrowDropDown />
                  </span>
                </Dropdown.Toggle>
                <Dropdown.Menu>
                  {Array.from(SNAPSHOT_CONFIGS.values()).map((config) => (
                    <li>
                      <UISref to="home.applications.application.snapshots" params={{ branch: config.gitBranch }}>
                        <a>{config.label}</a>
                      </UISref>
                    </li>
                  ))}
                </Dropdown.Menu>
              </Dropdown>
            </div>
          </div>
          {status === 'RESOLVED' || builds.length ? (
            <SnapshotsTable
              builds={builds}
              expandedBuilds={expandedBuilds}
              expandBuild={expandBuild}
              collapseBuild={collapseBuild}
              toggleDeploy={setDeployBuild}
            />
          ) : (
            <LoaderIcon />
          )}
        </InfiniteScroll>
      </div>
    </>
  );
}

interface SnapshotDeployModalProps {
  pipeline: string;
  setPipeline: (pipeline: string) => void;
  image: string;
  onClose: () => void;
  onSubmit: (command: IPipelineCommand) => PromiseLike<void>;
}

const SnapshotDeployModal = ({ pipeline, setPipeline, image, onClose, onSubmit }: SnapshotDeployModalProps) => {
  const [hours, setHours] = useState(1);
  const [name, setName] = useState('');
  const [environment, setEnvironment] = useState('qa');

  const environmentLabels = new Map([
    ['qa', 'QA/staging'],
    ['readonly', 'Production (readonly)'],
    ['readwrite', 'Production (read/write)'],
  ]);

  const command: IPipelineCommand = {
    pipelineName: pipeline,
    parameters: { image, version: image.replace(DOCKER_PREFIX, ''), name },
    trigger: {
      enabled: true,
      type: 'manual',
      parameters: { image, version: image.replace(DOCKER_PREFIX, ''), name, hours, environment }, // Ignore error, using for pipelines
    },
  };
  return (
    <Modal show={Boolean(image)} onHide={onClose} className="snapshot-modal">
      <ModalClose dismiss={onClose} />
      <Modal.Header>
        <Modal.Title>Deploy build</Modal.Title>
      </Modal.Header>
      <ModalBody>
        <label>Select a pipeline</label>
        <Dropdown>
          <Dropdown.Toggle>
            <span style={{ display: 'flex', alignItems: 'center' }}>
              {`Pipeline: ${pipeline}`}
              <ArrowDropDown />
            </span>
          </Dropdown.Toggle>
          <Dropdown.Menu>
            {Array.from(SNAPSHOT_CONFIGS.values()).map((config) => (
              <Dropdown.Item>
                <li>
                  <a onClick={() => setPipeline(config.gitBranch)}>{config.label}</a>
                </li>
              </Dropdown.Item>
            ))}
          </Dropdown.Menu>
        </Dropdown>
        {pipeline === NEXT_PREVIEW && (
          <>
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                paddingTop: '16px',
              }}
            >
              <div style={{ display: 'flex', width: '70%' }}>
                <Form>
                  <Form.Group className="mb-3" controlId="formBasicEmail">
                    <Form.Label>Name</Form.Label>
                    <Form.Control
                      style={{ maxWidth: '200px' }}
                      type="name"
                      placeholder="Enter name"
                      onChange={(e) => setName(e.target.value.trim())}
                    />
                    <Form.Text className="text-muted">
                      You can access this at{' '}
                      <a target="_blank" href={'https://' + name + '.next-preview.ziphq.com'}>
                        {name}.next-preview.ziphq.com
                      </a>
                    </Form.Text>
                  </Form.Group>
                </Form>
              </div>
              <div style={{ display: 'flex', width: '30%', flexDirection: 'column' }}>
                <label>Number of hours</label>
                <Dropdown>
                  <Dropdown.Toggle>
                    <span style={{ display: 'flex', alignItems: 'center' }}>
                      {`Hours: ${hours}`}
                      <ArrowDropDown />
                    </span>
                  </Dropdown.Toggle>
                  <Dropdown.Menu>
                    {Array.from([1, 2, 3, 4]).map((hrs) => (
                      <Dropdown.Item>
                        <li>
                          <a onClick={() => setHours(hrs)}>{hrs}</a>
                        </li>
                      </Dropdown.Item>
                    ))}
                  </Dropdown.Menu>
                </Dropdown>
              </div>
            </div>
            <div style={{ marginTop: '16px' }}>
              <label>Environment</label>
              <Dropdown>
                <Dropdown.Toggle>
                  <span style={{ display: 'flex', alignItems: 'center' }}>
                    {environmentLabels.get(environment)}
                    <ArrowDropDown />
                  </span>
                </Dropdown.Toggle>
                <Dropdown.Menu>
                  {Array.from(environmentLabels).map(([key, value]) => (
                    <Dropdown.Item onClick={() => setEnvironment(key)}>
                      <li>
                        <a>{value}</a>
                      </li>
                    </Dropdown.Item>
                  ))}
                </Dropdown.Menu>
              </Dropdown>
            </div>
          </>
        )}
      </ModalBody>
      <Modal.Footer>
        <Button onClick={() => onSubmit(command)}>Deploy</Button>
      </Modal.Footer>
    </Modal>
  );
};
