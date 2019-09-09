import React, { useMemo } from 'react';
import { observer } from 'mobx-react-lite';
import { Modal } from 'choerodon-ui/pro';
import { handlePromptError } from '../../../../../../utils';
import HeaderButtons from '../../../../../../components/header-buttons';
import DetailsModal from './details';
import ValueModalContent from './values/Config';
import UpgradeModalContent from './values/Upgrade';
import { useResourceStore } from '../../../../stores';
import { useInstanceStore } from '../stores';

const detailKey = Modal.key();
const valuesKey = Modal.key();
const upgradeKey = Modal.key();

const EnvModals = observer(() => {
  const {
    prefixCls,
    intlPrefix,
    intl: { formatMessage },
    resourceStore,
    treeDs,
  } = useResourceStore();
  const {
    baseDs,
    casesDs,
    podsDs,
    istStore,
    detailsStore,
    tabs: {
      CASES_TAB,
      DETAILS_TAB,
      PODS_TAB,
    },
    AppState: { currentMenuType: { id: projectId } },
  } = useInstanceStore();
  const modalStyle = useMemo(() => ({
    width: 'calc(100vw - 3.52rem)',
  }), []);

  function openValueModal() {
    const record = baseDs.current;
    if (!record) return false;

    const { id, parentId } = resourceStore.getSelectedMenu;
    const appServiceVersionId = record.get('appServiceVersionId');
    istStore.loadValue(projectId, id, appServiceVersionId);

    const deployVo = {
      id,
      parentId,
      projectId,
      appServiceVersionId,
    };
    Modal.open({
      key: valuesKey,
      title: formatMessage({ id: `${intlPrefix}.modal.values` }),
      drawer: true,
      okText: formatMessage({ id: 'deployment' }),
      cancelText: formatMessage({ id: 'close' }),
      style: modalStyle,
      children: <ValueModalContent
        store={istStore}
        vo={deployVo}
        intlPrefix={intlPrefix}
        prefixCls={prefixCls}
        formatMessage={formatMessage}
      />,
      afterClose: () => {
        istStore.setUpgradeValue({});
      },
    });
  }

  function openUpgradeModal() {
    const record = baseDs.current;
    if (!record) return false;

    const { id, parentId } = resourceStore.getSelectedMenu;
    const appServiceVersionId = record.get('appServiceVersionId');
    const deployVo = {
      id,
      parentId,
      versionId: appServiceVersionId,
    };

    Modal.open({
      key: upgradeKey,
      title: formatMessage({ id: `${intlPrefix}.modal.modify` }),
      drawer: true,
      okText: formatMessage({ id: 'modify' }),
      cancelText: formatMessage({ id: 'close' }),
      style: modalStyle,
      children: <UpgradeModalContent
        store={istStore}
        vo={deployVo}
        intlPrefix={intlPrefix}
        prefixCls={prefixCls}
      />,
      afterClose: () => {
        istStore.setUpgradeValue({});
      },
    });
  }

  function openDetailModal() {
    Modal.open({
      key: detailKey,
      title: formatMessage({ id: `${intlPrefix}.modal.detail` }),
      drawer: true,
      okCancel: false,
      okText: formatMessage({ id: 'close' }),
      style: { width: 380 },
      children: <DetailsModal
        record={baseDs.current} 
        intlPrefix={intlPrefix}
        prefixCls={prefixCls}
        formatMessage={formatMessage}
      />,
    });
  }

  function getDs(key) {
    const dsMapping = {
      [CASES_TAB]: casesDs,
      [PODS_TAB]: podsDs,
    };
    return dsMapping[key];
  }

  function refresh() {
    const activeKey = istStore.getTabKey;
    const { id } = resourceStore.getSelectedMenu;
    baseDs.query();
    treeDs.query();
    if (activeKey === DETAILS_TAB) {
      detailsStore.loadResource(projectId, id);
    } else {
      const ds = getDs(activeKey);
      ds && ds.query();
    }
  }

  async function redeploy() {
    const { id } = resourceStore.getSelectedMenu;
    try {
      const result = await istStore.redeploy(projectId, id);
      if (handlePromptError(result, false)) {
        refresh();
      }
    } catch (e) {
      Choerodon.handleResponseError(e);
    }
  }

  function getHeader() {
    const { id } = resourceStore.getSelectedMenu;
    const btnDisabled = !id;

    const buttons = [{
      name: formatMessage({ id: `${intlPrefix}.modal.values` }),
      icon: 'rate_review1',
      handler: openValueModal,
      display: true,
      permissions: [],
      group: 1,
      disabled: btnDisabled,
    }, {
      name: formatMessage({ id: `${intlPrefix}.modal.modify` }),
      icon: 'backup_line',
      handler: openUpgradeModal,
      permissions: [],
      display: true,
      group: 1,
      disabled: btnDisabled,
    }, {
      name: formatMessage({ id: `${intlPrefix}.modal.redeploy` }),
      icon: 'redeploy_line',
      handler: redeploy,
      permissions: [],
      display: true,
      group: 1,
      disabled: btnDisabled,
    }, {
      name: formatMessage({ id: `${intlPrefix}.modal.detail` }),
      icon: 'find_in_page',
      handler: openDetailModal,
      permissions: [],
      display: true,
      group: 2,
      disabled: btnDisabled,
    }, {
      name: formatMessage({ id: 'refresh' }),
      icon: 'refresh',
      handler: refresh,
      display: true,
      group: 2,
      disabled: btnDisabled,
    }];

    return <HeaderButtons items={buttons} />;
  }

  return getHeader();
});

export default EnvModals;
