import { useState, useEffect, memo } from "react";
import { Table, Button, Modal, message, Space, Typography, Tag } from "antd";
import {
  DeleteOutlined,
  EyeOutlined,
  ExclamationCircleOutlined,
} from "@ant-design/icons";
import { useNavigate } from "react-router-dom";
import { getSessions, deleteSession, Session } from "@/api/chat";
import dayjs from "dayjs";
import type { TablePaginationConfig } from "antd";

const { Title } = Typography;
const { confirm } = Modal;

type ChatHistoryProps = Record<string, never>;

/**
 * 会话历史页面组件
 * 显示用户的所有对话会话
 * 支持分页、删除和查看功能
 */
const ChatHistory: GenieType.FC<ChatHistoryProps> = memo(() => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [sessions, setSessions] = useState<Session[]>([]);
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  });

  /**
   * 加载会话列表
   */
  const loadSessions = async (page = 1, pageSize = 50) => {
    try {
      setLoading(true);
      const response = await getSessions({
        page,
        pageSize,
      });

      setSessions(response.list);
      setPagination({
        current: response.page,
        pageSize: response.pageSize,
        total: response.total,
      });
    } catch (error: any) {
      message.error(error.message || "加载会话列表失败");
    } finally {
      setLoading(false);
    }
  };

  /**
   * 初始加载
   */
  useEffect(() => {
    loadSessions();
  }, []);

  /**
   * 处理分页变化
   */
  const handleTableChange = (newPagination: TablePaginationConfig) => {
    loadSessions(newPagination.current, newPagination.pageSize);
  };

  /**
   * 查看会话详情（跳转到首页并加载该会话）
   */
  const handleView = (record: Session) => {
    // 跳转到首页，并通过URL参数传递sessionId（使用业务标识符而非数据库主键）
    navigate(`/?sessionId=${record.sessionId}`);
  };

  /**
   * 删除会话
   */
  const handleDelete = (record: Session) => {
    confirm({
      title: "确认删除",
      icon: <ExclamationCircleOutlined />,
      content: `确定要删除会话"${record.title}"吗？此操作不可恢复。`,
      okText: "确认",
      okType: "danger",
      cancelText: "取消",
      onOk: async () => {
        try {
          await deleteSession(record.id);
          message.success("删除成功");
          // 重新加载当前页
          loadSessions(pagination.current, pagination.pageSize);
        } catch (error: any) {
          message.error(error.message || "删除失败");
        }
      },
    });
  };

  /**
   * 表格列配置
   */
  const columns = [
    {
      title: "会话标题",
      dataIndex: "title",
      key: "title",
      ellipsis: true,
      width: "35%",
      render: (text: string) => (
        <span className="text-[#333] font-medium">{text || "未命名会话"}</span>
      ),
    },
    {
      title: "消息数",
      dataIndex: "messageCount",
      key: "messageCount",
      width: "10%",
      align: "center" as const,
      render: (count: number) => <Tag color="blue">{count || 0}</Tag>,
    },
    {
      title: "创建时间",
      dataIndex: "createTime",
      key: "createTime",
      width: "20%",
      render: (time: string) => dayjs(time).format("YYYY-MM-DD HH:mm:ss"),
    },
    {
      title: "更新时间",
      dataIndex: "updateTime",
      key: "updateTime",
      width: "20%",
      render: (time: string) => dayjs(time).format("YYYY-MM-DD HH:mm:ss"),
    },
    {
      title: "操作",
      key: "action",
      width: "15%",
      align: "center" as const,
      render: (_: any, record: Session) => (
        <Space size="small">
          <Button
            type="link"
            icon={<EyeOutlined />}
            onClick={() => handleView(record)}
            className="text-[#4040ff]"
          >
            查看
          </Button>
          <Button
            type="link"
            danger
            icon={<DeleteOutlined />}
            onClick={() => handleDelete(record)}
          >
            删除
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <div className="h-full p-6 bg-[#f5f5f5]">
      <div className="max-w-7xl mx-auto">
        {/* 页面标题 */}
        <div className="bg-white rounded-lg shadow-sm p-6 mb-4">
          <Title
            level={3}
            className="!mb-2"
          >
            <span className="bg-gradient-to-r from-[#4040ff] to-[#764ba2] bg-clip-text text-transparent">
              会话历史
            </span>
          </Title>
          <p className="text-[#666] text-sm">查看和管理您的所有对话会话</p>
        </div>

        {/* 会话列表 */}
        <div className="bg-white rounded-lg shadow-sm p-6">
          <Table
            dataSource={sessions}
            columns={columns}
            rowKey="id"
            loading={loading}
            pagination={{
              ...pagination,
              showSizeChanger: true,
              showQuickJumper: true,
              showTotal: (total) => `共 ${total} 条记录`,
              pageSizeOptions: ["10", "20", "50", "100"],
            }}
            onChange={handleTableChange}
            className="chat-history-table"
          />
        </div>
      </div>

      {/* 全局样式 */}
      <style>{`
        .chat-history-table .ant-table-thead > tr > th {
          background-color: #fafafa;
          color: #333;
          font-weight: 600;
        }

        .chat-history-table .ant-table-tbody > tr:hover > td {
          background-color: rgba(64, 64, 255, 0.03);
        }

        .chat-history-table .ant-pagination-item-active {
          border-color: #4040ff;
        }

        .chat-history-table .ant-pagination-item-active a {
          color: #4040ff;
        }
      `}</style>
    </div>
  );
});

ChatHistory.displayName = "ChatHistory";

export default ChatHistory;
