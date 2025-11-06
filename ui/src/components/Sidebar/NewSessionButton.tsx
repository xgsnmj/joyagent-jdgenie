import { memo } from "react";
import { Button } from "antd";
import { PlusOutlined } from "@ant-design/icons";

/**
 * 新建会话按钮组件Props
 */
interface NewSessionButtonProps {
  /** 按钮是否可用 */
  enabled: boolean;
  /** 点击回调函数 */
  onClick: () => void;
}

/**
 * 新建会话按钮组件
 * 负责显示新建会话按钮和处理点击事件
 */
const NewSessionButton: GenieType.FC<NewSessionButtonProps> = memo(
  ({ enabled, onClick }) => {
    return (
      <Button
        type="primary"
        icon={<PlusOutlined className="text-xl font-black" />}
        block
        size="large"
        onClick={onClick}
        disabled={!enabled}
      >
        <span className="relative z-10 font-black text-lg">新建会话</span>
        <span className="ml-2 text-sm opacity-75 font-medium relative z-10">
          Ctrl K
        </span>
        {enabled && (
          <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/20 to-transparent translate-x-[-100%] group-hover:translate-x-[100%] transition-transform duration-700"></div>
        )}
      </Button>
    );
  }
);

NewSessionButton.displayName = "NewSessionButton";

export default NewSessionButton;
