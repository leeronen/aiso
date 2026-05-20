import { Modal, type ModalProps } from 'antd';

export type AppModalProps = Omit<ModalProps, 'destroyOnClose'>;

/** Form / CRUD modals: forceRender + destroyOnHidden (antd 5.25+). Do not use destroyOnClose. */
export function AppModal({
  forceRender = true,
  destroyOnHidden = false,
  ...props
}: AppModalProps) {
  return <Modal forceRender={forceRender} destroyOnHidden={destroyOnHidden} {...props} />;
}
