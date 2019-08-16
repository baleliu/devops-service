import React, { Fragment, useEffect, lazy, Suspense, useMemo } from 'react';
import { observer } from 'mobx-react-lite';
import { Tabs } from 'choerodon-ui';
import isEmpty from 'lodash/isEmpty';
import PrefixTitle from '../../components/prefix-title';
import PodCircle from '../../components/pod-circle';
import Modals from './modals';
import { useResourceStore } from '../../../stores';
import { useMainStore } from '../../stores';
import { useInstanceStore } from './stores';

import './index.less';

const { TabPane } = Tabs;

const Cases = lazy(() => import('./cases'));
const Details = lazy(() => import('./details'));
const PodsDetails = lazy(() => import('./pods-details'));

const InstanceContent = observer(() => {
  const {
    prefixCls,
    intlPrefix,
  } = useResourceStore();
  const {
    podColor: {
      RUNNING_COLOR,
      PADDING_COLOR,
    },
  } = useMainStore();
  const {
    intl: { formatMessage },
    tabs: {
      CASES_TAB,
      DETAILS_TAB,
      PODS_TAB,
    },
    istStore,
  } = useInstanceStore();
  const podSize = useMemo(() => ({
    width: 22,
    height: 22,
  }), []);

  function handleChange(key) {
    istStore.setTabKey(key);
  }

  function getTitle() {
    // const { code, podRunningCount, podCount } = detail;
    // const podUnlinkCount = podCount - podRunningCount;

    // return <Fragment>
    //   <PodCircle
    //     style={podSize}
    //     dataSource={[{
    //       name: 'running',
    //       value: podRunningCount,
    //       stroke: RUNNING_COLOR,
    //     }, {
    //       name: 'unlink',
    //       value: podUnlinkCount,
    //       stroke: PADDING_COLOR,
    //     }]}
    //   />
    //   <span className={`${prefixCls}-title-text`}>{code}</span>
    // </Fragment>;
  }

  return (
    <div className={`${prefixCls}-instance`}>
      <Modals />
      <PrefixTitle
        prefixCls={prefixCls}
        fallback
      >
        {/* getTitle() */}
      </PrefixTitle>
      <Tabs
        className={`${prefixCls}-environment-tabs`}
        animated={false}
        activeKey={istStore.getTabKey}
        onChange={handleChange}
      >
        <TabPane
          key={CASES_TAB}
          tab={formatMessage({ id: `${intlPrefix}.instance.tabs.cases` })}
        >
          <Suspense fallback={<div>loading</div>}>
            <Cases />
          </Suspense>
        </TabPane>
        <TabPane
          key={DETAILS_TAB}
          tab={formatMessage({ id: `${intlPrefix}.instance.tabs.details` })}
        >
          <Suspense fallback={<div>loading</div>}>
            <Details />
          </Suspense>
        </TabPane>
        <TabPane
          key={PODS_TAB}
          tab={formatMessage({ id: `${intlPrefix}.instance.tabs.pods` })}
        >
          <Suspense fallback={<div>loading</div>}>
            <PodsDetails />
          </Suspense>
        </TabPane>
      </Tabs>
    </div>
  );
});

export default InstanceContent;