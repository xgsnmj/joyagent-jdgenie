import { memo } from "react";
import { DeleteOutlined } from "@ant-design/icons";
import { Modal } from "antd";
import { formatDistanceToNow } from "date-fns";
import { zhCN } from "date-fns/locale";

/**
 * SessionItem组件Props
 */
interface SessionItemProps {
  session: CHAT.Session;
  isActive: boolean;
  onClick: () => void;
  onDelete: () => void;
}

/**
 * 会话列表项组件 - Material Design 风格
 * 显示单个会话信息，支持点击选中和删除
 * 遵循 Material Design 卡片设计原则
 */
const SessionItem: GenieType.FC<SessionItemProps> = memo(
  ({ session, isActive, onClick, onDelete }) => {
    /**
     * 处理删除确认
     */
    const handleDelete = (e: React.MouseEvent) => {
      e.stopPropagation();

      Modal.confirm({
        title: "确认删除",
        content: `确定要删除会话"${session.title}"吗？`,
        okText: "删除",
        cancelText: "取消",
        okButtonProps: {
          danger: true,
          className: "bg-red-600 hover:bg-red-700 border-red-600",
        },
        onOk: onDelete,
      });
    };

    /**
     * 格式化时间显示 - Material Design 风格
     */
    const formatTime = (time: string) => {
      try {
        const date = new Date(time);
        return formatDistanceToNow(date, { addSuffix: true, locale: zhCN });
      } catch {
        return time;
      }
    };

    return (
      <div
        className={`flex flex-col
        relative group cursor-pointer transition-all duration-300 rounded-4
        ${
          isActive
            ? "bg-blue-50 text-blue-900 border-2 border-blue-200 "
            : "bg-white hover:bg-gray-50 text-gray-900 border border-gray-200 hover:shadow-md hover:border-gray-300"
        }
      `}
        onClick={onClick}
        style={{
          // Material Design 卡片样式
          margin: "4px 0",
          minHeight: "48px",
          transition: "all 0.2s cubic-bezier(0.4, 0, 0.2, 1)",
        }}
      >
        {/* 删除按钮 - 右上角悬停显示 */}
        <button
          onClick={handleDelete}
          className={`
            absolute top-2 right-2 z-10
            opacity-0 group-hover:opacity-100 transition-all duration-200 transform
            group-hover:scale-100 scale-95
            p-1.5 rounded-full hover:bg-red-50 hover:shadow-sm
            text-gray-400 hover:text-red-600
            flex items-center justify-center
            ${isActive ? "hover:bg-red-100" : ""}
          `}
          title="删除会话"
          aria-label="删除会话"
        >
          <DeleteOutlined className="text-xs" />
        </button>

        <div className="flex flex-col p-6 flex-1 pr-10">
          <div className="flex-1 min-w-0">
            {/* 标题 */}
            <div
              className={`mb-1 truncate text-sm
            ${isActive ? "text-blue-900" : "text-gray-900"}
          `}
            >
              {session.title || "新对话"}
            </div>

            {/* 元数据区域 */}
            <div className="flex items-center justify-between gap-2">
              {/* 时间显示 */}
              <div className="flex items-center gap-1 text-sm text-gray-500">
                <span className="text-xs">
                  {formatTime(session.updateTime)}
                </span>
              </div>

              {/* 消息数量徽章 */}
              {session.messageCount !== undefined &&
                session.messageCount > 0 && (
                  <div
                    className={`
                inline-flex items-center px-2 py-1 rounded-full text-xs font-medium transition-all
                ${
                  isActive
                    ? "bg-blue-100 text-blue-800"
                    : "bg-gray-100 text-gray-600 group-hover:bg-gray-200"
                }
              `}
                  >
                    {Math.floor(session.messageCount / 2)} 条消息
                  </div>
                )}
            </div>
          </div>
        </div>
      </div>
    );
  }
);

SessionItem.displayName = "SessionItem";

export default SessionItem;
