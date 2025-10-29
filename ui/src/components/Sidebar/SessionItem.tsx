import { memo, useState } from 'react';
import { DeleteOutlined, MessageOutlined } from '@ant-design/icons';
import { Modal } from 'antd';
import { Session } from '@/api/chat';
import { formatDistanceToNow } from 'date-fns';
import { zhCN } from 'date-fns/locale';

/**
 * SessionItem组件Props
 */
interface SessionItemProps {
  session: Session;
  isActive: boolean;
  onClick: () => void;
  onDelete: () => void;
}

/**
 * 会话列表项组件
 * 显示单个会话信息，支持点击选中和删除
 */
const SessionItem: GenieType.FC<SessionItemProps> = memo(({ session, isActive, onClick, onDelete }) => {
  const [showDelete, setShowDelete] = useState(false);

  /**
   * 处理删除按钮点击
   */
  const handleDelete = (e: React.MouseEvent) => {
    e.stopPropagation(); // 阻止事件冒泡

    Modal.confirm({
      title: '确认删除',
      content: `确定要删除会话"${session.title}"吗？`,
      okText: '删除',
      cancelText: '取消',
      okButtonProps: { danger: true },
      onOk: onDelete,
    });
  };

  /**
   * 格式化时间显示
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
      className={`
        group relative px-3.5 py-3 rounded-2xl cursor-pointer transition-all duration-300
        ${isActive
          ? 'bg-gradient-to-br from-[#4040ff] via-[#5050ff] to-[#6060ff] text-white shadow-xl shadow-[#4040ff]/40 scale-[1.02] ring-2 ring-[#4040ff]/20'
          : 'bg-white/80 hover:bg-white hover:shadow-lg hover:shadow-gray-200/50 text-gray-700 border border-gray-100/80 hover:scale-[1.01]'
        }
      `}
      onClick={onClick}
      onMouseEnter={() => setShowDelete(true)}
      onMouseLeave={() => setShowDelete(false)}
    >
      {/* 会话内容 */}
      <div className="flex items-center gap-3">
        {/* 图标 */}
        <div className={`
          w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0 transition-all
          ${isActive
            ? 'bg-white/25 shadow-lg shadow-black/10'
            : 'bg-gradient-to-br from-[#4040ff]/10 to-[#764ba2]/10 group-hover:from-[#4040ff]/15 group-hover:to-[#764ba2]/15'
          }
        `}>
          <MessageOutlined className={`text-base ${isActive ? 'text-white' : 'text-[#4040ff]'}`} />
        </div>

        {/* 会话信息 */}
        <div className="flex-1 min-w-0">
          <div className={`text-sm font-bold truncate mb-1.5 leading-tight ${isActive ? 'text-white' : 'text-gray-900'}`}>
            {session.title || '新对话'}
          </div>
          <div className="flex items-center justify-between gap-2">
            <span className={`text-xs flex items-center gap-1.5 ${isActive ? 'text-white/80' : 'text-gray-500'}`}>
              <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              {formatTime(session.updateTime)}
            </span>
            {session.messageCount !== undefined && session.messageCount > 0 && (
              <span className={`
                text-xs px-2.5 py-1 rounded-full font-semibold flex items-center gap-1 transition-all
                ${isActive
                  ? 'bg-white/25 text-white shadow-md'
                  : 'bg-gradient-to-r from-[#4040ff]/15 to-[#764ba2]/15 text-[#4040ff] group-hover:from-[#4040ff]/20 group-hover:to-[#764ba2]/20'
                }
              `}>
                <svg className="w-3 h-3" fill="currentColor" viewBox="0 0 20 20">
                  <path d="M2 5a2 2 0 012-2h7a2 2 0 012 2v4a2 2 0 01-2 2H9l-3 3v-3H4a2 2 0 01-2-2V5z" />
                  <path d="M15 7v2a4 4 0 01-4 4H9.828l-1.766 1.767c.28.149.599.233.938.233h2l3 3v-3h2a2 2 0 002-2V9a2 2 0 00-2-2h-1z" />
                </svg>
                {session.messageCount}
              </span>
            )}
          </div>
        </div>
      </div>

      {/* 删除按钮 */}
      {showDelete && !isActive && (
        <button
          className="absolute right-2 top-2 p-2 rounded-xl bg-white hover:bg-red-50 border border-gray-200 hover:border-red-200 transition-all shadow-sm hover:shadow-md group/delete"
          onClick={handleDelete}
          title="删除会话"
        >
          <DeleteOutlined className="text-xs text-gray-600 group-hover/delete:text-red-500 transition-colors" />
        </button>
      )}
    </div>
  );
});

SessionItem.displayName = 'SessionItem';

export default SessionItem;
