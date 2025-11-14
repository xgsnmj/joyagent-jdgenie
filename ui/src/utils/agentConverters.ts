/**
 * 智能体流式输出格式转换工具
 * 用于将不同智能体提供商的流式输出格式统一转换为系统标准的 MESSAGE.Answer 格式
 * 
 * @author yangyj
 * @since 2025-01-03
 */

import { useAgentProviderStore } from '@/store/agentProvider';

/**
 * 获取当前选中的智能体配置
 */
function getCurrentProvider() {
  const state = useAgentProviderStore.getState();
  return state.currentProvider;
}

/**
 * Coze流式输出数据结构
 */
export interface CozeStreamData {
  id: string;
  conversation_id: string;
  bot_id: string;
  role: string;
  type: string;
  content: string;
  content_type: string;
  chat_id: string;
  section_id: string;
  created_at?: number;
  updated_at?: number;
  time_cost?: {
    total_duration_ms: number;
  };
}

/**
 * 通义千问流式输出数据结构
 */
export interface TongyiStreamData {
  versionId: string;
  modelId: string;
  threadId: string;
  replayId: string;
  role: string;
  roleDisplayName: string;
  messageType: string;
  contentGroup: string;
  content: string;
  fullContent: string | null;
  jsonContent: any | null;
  contentType: string;
  extraInfo: any | null;
  stop: boolean;
  time: string;
  inputTokens: number;
  outputTokens: number;
  totalTokens: number;
  workflowId: string | null;
}

/**
 * 融汇流式输出数据结构
 */
export interface RonghuiStreamData {
  id: string;
  request_id: string;
  app_id: string;
  session_id: string;
  question: string;
  inputs: any;
  group_id: string;
  message: {
    code: number;
    message: string;
    status: number;
  };
  sender: 'system' | 'ai';
  result_type: string;
  action: 'roger' | 'region_begin' | 'intermediate' | 'region_finish' | 'finish';
  output_mode: 'stream' | 'stream_finish' | 'non_stream';
  data_offset: number;
  data?: string;
  region_type?: string;
  region_name?: string;
  region_id?: string;
  graph_id?: string;
  node_id?: string;
  node_code?: string;
  node_type?: string;
  node_name?: string;
  stream?: string;
  reference?: any;
  create_at?: number;
  version_id?: string;
  runtime_file_infos?: any[];
}

/**
 * 智能体转换器接口
 * 所有智能体转换器都需要实现这个接口
 */
export interface AgentConverter<T = any> {
  /**
   * 将智能体的流式输出转换为 MESSAGE.Answer 格式
   * @param data 智能体的原始数据
   * @param event SSE 事件类型
   * @returns 转换后的 MESSAGE.Answer 对象
   */
  convert: (data: T, event: string) => MESSAGE.Answer;
  
  /**
   * 重置转换器状态（用于新会话）
   */
  reset?: () => void;
}

/**
 * MESSAGE.Answer 构建参数
 */
interface AnswerBuilderParams {
  response: string;
  responseAll: string;
  finished: boolean;
  useTimes?: number;
  useTokens?: number;
  traceId?: string;
  reqId?: string;
  packageType?: 'data' | 'heartbeat';
  // 可选的结构化数据
  eventData?: {
    messageTime: string;
    messageId: string;
    taskId: string;
    requestId: string;
    result: string;
    isFinal: boolean;
  };
}

/**
 * 构建标准的 MESSAGE.Answer 对象
 * 提取公共逻辑，减少重复代码
 */
function buildAnswer(params: AnswerBuilderParams): MESSAGE.Answer {
  const {
    response,
    responseAll,
    finished,
    useTimes = 0,
    useTokens = 0,
    traceId = '',
    reqId = '',
    packageType = 'data',
    eventData,
  } = params;

  const answer: MESSAGE.Answer = {
    status: 'success',
    response,
    responseAll,
    finished,
    useTimes,
    useTokens,
    resultMap: {
      steps: [],
    },
    responseType: 'text',
    voiceUrl: '',
    traceId,
    reqId,
    encrypted: false,
    runningLog: '',
    query: '',
    messages: '',
    packageType,
    errorMsg: '',
  };

  // 如果提供了事件数据，添加到 resultMap 中
  if (eventData) {
    answer.resultMap.eventData = {
      messageOrder: 0,
      messageType: 'task',
      resultMap: {
        messageTime: eventData.messageTime,
        messageType: 'result',
        result: eventData.result,
        isFinal: eventData.isFinal,
        requestId: eventData.requestId,
        messageId: eventData.messageId,
        finish: eventData.isFinal,
        id: eventData.messageId,
        resultMap: {
          steps: [],
          taskSummary: eventData.result,
        },
      },
      messageId: eventData.messageId,
      taskId: eventData.taskId,
      taskOrder: 0,
    };
  }

  return answer;
}

/**
 * 基础转换器抽象类
 * 提供公共的累积内容管理功能
 */
abstract class BaseStreamConverter<T> implements AgentConverter<T> {
  protected accumulatedContent = '';

  abstract convert(data: T, event: string): MESSAGE.Answer;

  /**
   * 重置累积内容
   */
  reset(): void {
    this.accumulatedContent = '';
  }

  /**
   * 累积内容
   */
  protected accumulate(content: string): void {
    this.accumulatedContent += content;
  }

  /**
   * 创建增量消息响应
   */
  protected createIncrementalResponse(params: {
    incrementalContent: string;
    messageTime: string;
    messageId: string;
    taskId: string;
    traceId: string;
    reqId: string;
    useTokens?: number;
  }): MESSAGE.Answer {
    return buildAnswer({
      response: params.incrementalContent,
      responseAll: this.accumulatedContent,
      finished: false,
      useTokens: params.useTokens,
      traceId: params.traceId,
      reqId: params.reqId,
      packageType: 'data',
      eventData: {
        messageTime: params.messageTime,
        messageId: params.messageId,
        taskId: params.taskId,
        requestId: params.reqId,
        result: this.accumulatedContent,
        isFinal: false,
      },
    });
  }

  /**
   * 创建完成消息响应
   */
  protected createCompletedResponse(params: {
    finalContent: string;
    messageTime: string;
    messageId: string;
    taskId: string;
    traceId: string;
    reqId: string;
    useTimes?: number;
    useTokens?: number;
  }): MESSAGE.Answer {
    const result = buildAnswer({
      response: params.finalContent,
      responseAll: params.finalContent,
      finished: true,
      useTimes: params.useTimes,
      useTokens: params.useTokens,
      traceId: params.traceId,
      reqId: params.reqId,
      packageType: 'data',
      eventData: {
        messageTime: params.messageTime,
        messageId: params.messageId,
        taskId: params.taskId,
        requestId: params.reqId,
        result: params.finalContent,
        isFinal: true,
      },
    });
    
    // 重置累积内容
    this.reset();
    return result;
  }

  /**
   * 创建心跳包响应
   */
  protected createHeartbeatResponse(traceId?: string, reqId?: string): MESSAGE.Answer {
    return buildAnswer({
      response: '',
      responseAll: this.accumulatedContent,
      finished: false,
      traceId,
      reqId,
      packageType: 'heartbeat',
    });
  }

  /**
   * 创建 done 事件响应
   */
  protected createDoneResponse(): MESSAGE.Answer {
    const result = buildAnswer({
      response: '',
      responseAll: this.accumulatedContent,
      finished: true,
      packageType: 'heartbeat',
    });
    this.reset();
    return result;
  }
}

/**
 * Coze智能体转换器
 * 将Coze的流式输出转换为本地MESSAGE.Answer格式
 */
class CozeConverter extends BaseStreamConverter<CozeStreamData> {
  /**
   * 转换Coze流式输出为MESSAGE.Answer格式
   */
  convert(cozeData: CozeStreamData, event: string): MESSAGE.Answer {
    // conversation.message.delta事件 - 增量内容
    if (event === 'conversation.message.delta' && cozeData.type === 'answer') {
      this.accumulate(cozeData.content);
      return this.createIncrementalResponse({
        incrementalContent: cozeData.content,
        messageTime: new Date().toISOString(),
        messageId: cozeData.id,
        taskId: cozeData.conversation_id,
        traceId: cozeData.conversation_id,
        reqId: cozeData.chat_id,
      });
    }

    // conversation.message.completed事件 - 完成消息
    if (event === 'conversation.message.completed' && cozeData.type === 'answer') {
      return this.createCompletedResponse({
        finalContent: cozeData.content,
        messageTime: new Date().toISOString(),
        messageId: cozeData.id,
        taskId: cozeData.conversation_id,
        traceId: cozeData.conversation_id,
        reqId: cozeData.chat_id,
        useTimes: cozeData.time_cost?.total_duration_ms || 0,
      });
    }

    // conversation.chat.completed事件 - 会话完成
    if (event === 'conversation.chat.completed') {
      return this.createDoneResponse();
    }

    // done事件
    if (event === 'done') {
      return this.createDoneResponse();
    }

    // 其他类型的消息（follow_up、verbose等）暂时忽略，返回心跳包
    return this.createHeartbeatResponse(
      cozeData.conversation_id || '',
      cozeData.chat_id || ''
    );
  }
}

/**
 * 通义千问智能体转换器
 * 将通义千问的流式输出转换为本地MESSAGE.Answer格式
 */
class TongyiConverter extends BaseStreamConverter<TongyiStreamData> {
  /**
   * 转换通义千问流式输出为MESSAGE.Answer格式
   */
  convert(tongyiData: TongyiStreamData, event: string): MESSAGE.Answer {
    // data 事件（默认事件）或 message 事件 - 通义千问现在返回增量消息
    // 根据 messageType 判断是否为回答消息
    if ((event === '' || event === 'message' || event === 'data') && tongyiData.messageType === 'Answer') {
      // 如果是增量内容（stop=false），累积内容
      if (!tongyiData.stop) {
        this.accumulate(tongyiData.content);
        return this.createIncrementalResponse({
          incrementalContent: tongyiData.content,
          messageTime: tongyiData.time,
          messageId: tongyiData.replayId,
          taskId: tongyiData.threadId,
          traceId: tongyiData.threadId,
          reqId: tongyiData.replayId,
          useTokens: tongyiData.outputTokens || 0,
        });
      } 
      // 如果stop=true，表示这是最后一条消息
      else {
        // 使用fullContent作为完整内容（如果有的话）
        const finalContent = tongyiData.fullContent || this.accumulatedContent;
        return this.createCompletedResponse({
          finalContent,
          messageTime: tongyiData.time,
          messageId: tongyiData.replayId,
          taskId: tongyiData.threadId,
          traceId: tongyiData.threadId,
          reqId: tongyiData.replayId,
          useTokens: tongyiData.outputTokens || 0,
        });
      }
    }

    // done 事件
    if (event === 'done') {
      return this.createDoneResponse();
    }

    // 其他类型返回心跳包
    return this.createHeartbeatResponse(
      tongyiData.threadId || '',
      tongyiData.replayId || ''
    );
  }
}

/**
 * 融汇智能体转换器
 * 将融汇的流式输出转换为本地MESSAGE.Answer格式
 */
class RonghuiConverter extends BaseStreamConverter<RonghuiStreamData> {
  /**
   * 转换融汇流式输出为MESSAGE.Answer格式
   */
  convert(ronghuiData: RonghuiStreamData, event: string): MESSAGE.Answer {
    // 只处理 sender 为 'ai' 的消息
    if (ronghuiData.sender !== 'ai') {
      return this.createHeartbeatResponse(
        ronghuiData.session_id || '',
        ronghuiData.request_id || ''
      );
    }

    // intermediate 动作 - 增量内容
    if (ronghuiData.action === 'intermediate' && ronghuiData.data) {
      // data 字段本身就是增量内容，直接累加
      const incrementalContent = ronghuiData.data;
      this.accumulate(incrementalContent);
      console.log('incrementalContent', incrementalContent);

      // 如果是 stream_finish，表示这是最后一条增量消息，但不重置（可能还有后续事件）
      if (ronghuiData.output_mode === 'stream_finish') {
        return buildAnswer({
          response: this.accumulatedContent,
          responseAll: this.accumulatedContent,
          finished: true,
          traceId: ronghuiData.session_id,
          reqId: ronghuiData.request_id,
          packageType: 'data',
          eventData: {
            messageTime: ronghuiData.create_at ? new Date(ronghuiData.create_at).toISOString() : new Date().toISOString(),
            messageId: ronghuiData.id,
            taskId: ronghuiData.session_id,
            requestId: ronghuiData.request_id,
            result: this.accumulatedContent,
            isFinal: true,
          },
        });
      }

      // 普通增量消息
      return this.createIncrementalResponse({
        incrementalContent,
        messageTime: ronghuiData.create_at ? new Date(ronghuiData.create_at).toISOString() : new Date().toISOString(),
        messageId: ronghuiData.id,
        taskId: ronghuiData.session_id,
        traceId: ronghuiData.session_id,
        reqId: ronghuiData.request_id,
      });
    }

    // region_finish 动作 - 区域完成
    if (ronghuiData.action === 'region_finish') {
      return this.createCompletedResponse({
        finalContent: this.accumulatedContent,
        messageTime: ronghuiData.create_at ? new Date(ronghuiData.create_at).toISOString() : new Date().toISOString(),
        messageId: ronghuiData.id,
        taskId: ronghuiData.session_id,
        traceId: ronghuiData.session_id,
        reqId: ronghuiData.request_id,
      });
    }

    // finish 动作或 finish 事件 - 整个请求完成
    if (ronghuiData.action === 'finish' || event === 'finish') {
      return this.createCompletedResponse({
        finalContent: this.accumulatedContent,
        messageTime: ronghuiData.create_at ? new Date(ronghuiData.create_at).toISOString() : new Date().toISOString(),
        messageId: ronghuiData.id,
        taskId: ronghuiData.session_id,
        traceId: ronghuiData.session_id,
        reqId: ronghuiData.request_id,
      });
    }

    // done 事件
    if (event === 'done') {
      return this.createDoneResponse();
    }

    // 其他动作（roger, region_begin等）返回心跳包
    return this.createHeartbeatResponse(
      ronghuiData.session_id || '',
      ronghuiData.request_id || ''
    );
  }
}

/**
 * 智能体转换器工厂
 * 根据智能体类型返回对应的转换器实例
 */
class AgentConverterFactory {
  private converters: Map<string, AgentConverter> = new Map();

  constructor() {
    // 注册 Coze 转换器
    this.converters.set('coze', new CozeConverter());
    // 注册通义千问转换器
    this.converters.set('tongyi', new TongyiConverter());
    // 注册融汇转换器
    this.converters.set('ronghui', new RonghuiConverter());
  }

  /**
   * 获取指定类型的转换器
   * @param providerType 智能体提供商类型（如 'coze', 'openai' 等）
   * @returns 对应的转换器实例，如果不存在则返回 null
   */
  getConverter(providerType: string): AgentConverter | null {
    return this.converters.get(providerType) || null;
  }

  /**
   * 注册新的转换器
   * @param providerType 智能体提供商类型
   * @param converter 转换器实例
   */
  registerConverter(providerType: string, converter: AgentConverter) {
    this.converters.set(providerType, converter);
  }

  /**
   * 检查是否支持某个智能体类型
   * @param providerType 智能体提供商类型
   */
  hasConverter(providerType: string): boolean {
    return this.converters.has(providerType);
  }
}

// 导出单例工厂实例
export const agentConverterFactory = new AgentConverterFactory();

/**
 * 统一的智能体数据转换入口
 * 自动判断当前智能体类型，并根据类型选择合适的转换器
 * @param eventData SSE事件数据（字符串格式）
 * @param event SSE事件类型
 * @returns 转换后的 MESSAGE.Answer 对象
 */
export function convertStreamData(
  eventData: string,
  event: string
): MESSAGE.Answer | null {
  try {
    // 获取当前智能体配置
    const currentProvider = getCurrentProvider();
    const providerType = currentProvider?.providerType;

    console.log('当前智能体类型:', providerType, 'event:', event);

    // 如果没有配置或者是本地智能体，直接解析并返回
    if (!providerType || providerType === 'default') {
      try {
        const parsedData = JSON.parse(eventData);
        return parsedData as MESSAGE.Answer;
      } catch (error) {
        console.error('解析本地数据失败:', error);
        return null;
      }
    }

    // 获取对应的转换器
    const converter = agentConverterFactory.getConverter(providerType);
    if (!converter) {
      console.warn(`未找到 ${providerType} 类型的转换器，尝试使用原始数据`);
      try {
        const parsedData = JSON.parse(eventData);
        return parsedData as MESSAGE.Answer;
      } catch (error) {
        console.error('解析数据失败:', error);
        return null;
      }
    }

    // 处理特殊的[DONE]标记
    if (eventData === '[DONE]') {
      return converter.convert({} as any, 'done');
    }

    // 解析数据并转换
    const parsedData = JSON.parse(eventData);
    const convertedData = converter.convert(parsedData, event);
    console.log(`${providerType} 转换后数据:`, convertedData);
    
    return convertedData;
  } catch (error) {
    console.error('转换数据时发生错误:', error, 'eventData:', eventData);
    return null;
  }
}

/**
 * 便捷方法：转换智能体数据（保留用于直接调用）
 * @param providerType 智能体提供商类型
 * @param data 原始数据
 * @param event 事件类型
 * @returns 转换后的 MESSAGE.Answer 对象，如果没有对应转换器则返回 null
 * @deprecated 推荐使用 convertStreamData 方法，会自动判断智能体类型
 */
export function convertAgentData(
  providerType: string,
  data: any,
  event: string
): MESSAGE.Answer | null {
  const converter = agentConverterFactory.getConverter(providerType);
  if (!converter) {
    console.warn(`未找到 ${providerType} 类型的转换器`);
    return null;
  }
  return converter.convert(data, event);
}

