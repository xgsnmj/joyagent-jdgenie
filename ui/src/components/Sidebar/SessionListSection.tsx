import { memo } from "react";
import { Button, List, Empty, Skeleton, Typography, Space, Badge } from "antd";
import {
  HistoryOutlined,
  MessageOutlined,
  ClockCircleOutlined,
} from "@ant-design/icons";
import SessionItem from "./SessionItem";

const { Title, Text } = Typography;

/**
 * 会话列表区域组件Props
 */
interface SessionListSectionProps {
  /** 会话列表数据 */
  sessions: CHAT.Session[];
  /** 当前激活的会话ID */
  currentSessionId: string | null;
  /** 是否正在加载 */
  loading: boolean;
  /** 会话点击回调 */
  onSessionClick: (sessionId: string) => void;
  /** 会话删除回调 */
  onSessionDelete: (sessionId: string | number) => void;
  /** 查看全部历史回调 */
  onViewAllHistory: () => void;
}

/**
 * 会话列表区域组件
 * 负责显示会话列表标题、会话项目和空状态
 * 使用Ant Design组件重构，提供更好的用户体验和性能优化
 */
const SessionListSection: GenieType.FC<SessionListSectionProps> = memo(
  ({
    sessions,
    currentSessionId,
    loading,
    onSessionClick,
    onSessionDelete,
    onViewAllHistory,
  }) => {
    // Material Design 渲染优化：添加动画效果
    const renderSessionItem = (session: CHAT.Session) => (
      <List.Item
        className="!px-0 !py-0 !border-0"
        style={{
          // Material Design 列表项动画
          animation: "fadeInUp 0.3s ease-out",
          animationFillMode: "both",
        }}
      >
        <div
          className="w-full transition-all duration-200 hover:scale-[1.02]"
          style={{
            // Material Design 交互效果
            borderRadius: "12px",
            backgroundColor:
              session.sessionId === currentSessionId
                ? "#e3f2fd"
                : "transparent",
            transition: "all 0.2s cubic-bezier(0.4, 0, 0.2, 1)",
          }}
        >
          <SessionItem
            session={session}
            isActive={session.sessionId === currentSessionId}
            onClick={() => onSessionClick(session.sessionId)}
            onDelete={() => onSessionDelete(session.sessionId)}
          />
        </div>
      </List.Item>
    );

    return (
      <div className="h-full flex flex-col">
        {/* 列表标题区域 - Material Design 样式 */}
        <div className="px-3 py-3 mb-2 border-b border-gray-200/60 bg-white/40 backdrop-blur-sm">
          <div className="flex items-center justify-between">
            <Space
              align="center"
              size={12}
            >
              {/* Material Design 色彩指示器 */}
              <div className="w-2 h-6 bg-blue-600 rounded-full"></div>
              <Title
                level={5}
                className="!mb-0 !text-gray-900 !font-medium !tracking-tight"
              >
                最近对话
              </Title>
              {sessions.length > 0 && (
                <Badge
                  count={sessions.length}
                  showZero={false}
                  size="small"
                  style={{
                    backgroundColor: "#1976d2",
                    boxShadow:
                      "0 1px 3px rgba(0, 0, 0, 0.12), 0 1px 2px rgba(0, 0, 0, 0.24)",
                  }}
                />
              )}
            </Space>

            <Button
              type="text"
              size="small"
              icon={<HistoryOutlined />}
              onClick={onViewAllHistory}
              className="text-xs text-gray-600 hover:text-blue-600 hover:bg-blue-50/70 h-8 px-3 rounded-lg font-medium transition-all duration-200 hover:shadow-sm"
              title="查看全部历史会话"
            >
              全部
            </Button>
          </div>
        </div>

        {/* 会话列表内容 - Material Design 样式 */}
        <div className="flex-1 overflow-y-auto px-1 py-2">
          {loading ? (
            <LoadingState />
          ) : sessions.length === 0 ? (
            <EmptyState />
          ) : (
            <List
              dataSource={sessions}
              renderItem={renderSessionItem}
              split={false}
              className="space-y-2"
              size="small"
              locale={{ emptyText: null }}
              style={{
                // Material Design 列表样式
                padding: "4px 8px",
              }}
            />
          )}
        </div>
      </div>
    );
  }
);

/**
 * 加载状态组件
 * 使用Ant Design的Skeleton组件，应用Material Design样式
 */
const LoadingState: GenieType.FC = memo(() => (
  <div className="px-3 py-2">
    <Space
      direction="vertical"
      size={12}
      className="w-full"
    >
      {/* Material Design 骨架屏样式 */}
      {Array.from({ length: 5 }).map((_, index) => (
        <div
          key={index}
          className="bg-white rounded-xl p-4 border border-gray-100/80 shadow-sm hover:shadow-md transition-shadow duration-200"
          style={{
            // Material Design 卡片样式
            borderRadius: "12px",
            boxShadow:
              "0 1px 3px rgba(0, 0, 0, 0.12), 0 1px 2px rgba(0, 0, 0, 0.24)",
          }}
        >
          <Skeleton
            avatar={{
              shape: "circle",
              size: "default",
              style: {
                // Material Design 头像样式
                backgroundColor: "#e0e0e0",
              },
            }}
            title={{
              width: "60%",
              style: {
                // Material Design 标题样式
                height: "20px",
                borderRadius: "4px",
              },
            }}
            paragraph={{
              rows: 1,
              width: "80%",
              className: "!mb-0",
              style: {
                // Material Design 段落样式
                height: "16px",
                borderRadius: "4px",
              },
            }}
            active
            round
          />
        </div>
      ))}
    </Space>
  </div>
));

/**
 * 空状态组件
 * 使用Ant Design的Empty组件，应用Material Design样式
 */
const EmptyState: GenieType.FC = memo(() => (
  <div className="px-4 py-12">
    <div className="text-center">
      {/* Material Design 空状态图标 */}
      <div className="inline-flex items-center justify-center w-20 h-20 rounded-full bg-blue-50 mb-6 transition-all duration-300 hover:bg-blue-100">
        <MessageOutlined className="text-3xl text-blue-400" />
      </div>

      {/* Material Design 空状态文本 */}
      <div className="space-y-2">
        <Text className="text-base font-medium text-gray-700 block">
          暂无会话记录
        </Text>
        <div className="space-y-1">
          <Text
            type="secondary"
            className="text-sm text-gray-500 block"
          >
            开始您的第一次对话
          </Text>
          <Text
            type="secondary"
            className="text-xs text-gray-400"
          >
            点击"新建会话"按钮或按{" "}
            <Text className="!bg-gray-100 !px-2 !py-1 !rounded !text-xs">
              ⌘K
            </Text>
          </Text>
        </div>
      </div>

      {/* Material Design 建议按钮 */}
      <div className="mt-8">
        <Button
          type="primary"
          icon={<MessageOutlined />}
          className="h-10 px-6 rounded-full bg-blue-600 hover:bg-blue-700 border-0 shadow-md hover:shadow-lg transition-all duration-200"
          onClick={() => {
            // 可以触发新建会话
            console.log("创建新会话");
          }}
        >
          开始对话
        </Button>
      </div>
    </div>
  </div>
));

LoadingState.displayName = "LoadingState";
EmptyState.displayName = "EmptyState";
SessionListSection.displayName = "SessionListSection";

export default SessionListSection;
