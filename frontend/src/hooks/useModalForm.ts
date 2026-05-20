import type { FormInstance } from 'antd';
import { useEffect } from 'react';

/** Modal 打开后再写入表单，避免未挂载时触发 useForm 警告（配合 AppModal + forceRender） */
export function useSyncModalForm<T extends object>(
  open: boolean,
  form: FormInstance<T>,
  values: Partial<T> | null | undefined,
) {
  useEffect(() => {
    if (!open) return;
    form.resetFields();
    if (values) {
      form.setFieldsValue(values as T);
    }
  }, [open, form, values]);
}
