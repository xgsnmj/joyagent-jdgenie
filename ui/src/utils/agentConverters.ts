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
  threadId: string;
  traceId: string;
  versionId: string;
  inputTokens: number;
  outputTokens: number;
  response: {
    choices: Array<{
      finishReason: string;
      index: number;
      message: {
        content: string;
        role: string;
        roleDisplayName: string;
      };
    }>;
    created: number;
    id: string;
    modelId: string;
    time: string;
  };
  functionCallResponses?: any[];
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
 * Coze智能体转换器
 * 将Coze的流式输出转换为本地MESSAGE.Answer格式
 */
class CozeConverter implements AgentConverter<CozeStreamData> {
  // 用于累积content内容
  private accumulatedContent = '';

  /**
   * 转换Coze流式输出为MESSAGE.Answer格式
   */
  convert(cozeData: CozeStreamData, event: string): MESSAGE.Answer {
    // conversation.message.delta事件 - 增量内容
    if (event === 'conversation.message.delta' && cozeData.type === 'answer') {
      this.accumulatedContent += cozeData.content;
      return {
        status: 'success',
        response: cozeData.content, // 增量内容
        responseAll: this.accumulatedContent, // 累积的完整内容
        finished: false,
        useTimes: 0,
        useTokens: 0,
        resultMap: {
          steps: [],
          eventData: {
            messageOrder: 0,
            messageType: 'task',
            resultMap: {
              messageTime: new Date().toISOString(),
              messageType: 'result', // 使用 result 类型，显示在正文
              result: this.accumulatedContent, // 累积的完整内容
              isFinal: false,
              requestId: cozeData.chat_id,
              messageId: cozeData.id,
              finish: false,
              id: cozeData.id,
              resultMap: {
                steps: [],
                taskSummary: this.accumulatedContent, // 用于显示的内容
              },
            },
            messageId: cozeData.id,
            taskId: cozeData.conversation_id,
            taskOrder: 0,
          },
        },
        responseType: 'text',
        voiceUrl: '',
        traceId: cozeData.conversation_id,
        reqId: cozeData.chat_id,
        encrypted: false,
        runningLog: '',
        query: '',
        messages: '',
        packageType: 'data',
        errorMsg: '',
      };
    }

    // conversation.message.completed事件 - 完成消息
    if (event === 'conversation.message.completed' && cozeData.type === 'answer') {
      this.accumulatedContent = cozeData.content; // 使用完整内容
      return {
        status: 'success',
        response: cozeData.content,
        responseAll: cozeData.content,
        finished: true,
        useTimes: cozeData.time_cost?.total_duration_ms || 0,
        useTokens: 0,
        resultMap: {
          steps: [],
          eventData: {
            messageOrder: 0,
            messageType: 'task',
            resultMap: {
              messageTime: new Date().toISOString(),
              messageType: 'result',
              result: this.accumulatedContent, // 完整内容
              isFinal: true,
              requestId: cozeData.chat_id,
              messageId: cozeData.id,
              finish: true,
              id: cozeData.id,
              resultMap: {
                steps: [],
                taskSummary: this.accumulatedContent,
              },
            },
            messageId: cozeData.id,
            taskId: cozeData.conversation_id,
            taskOrder: 0,
          },
        },
        responseType: 'text',
        voiceUrl: '',
        traceId: cozeData.conversation_id,
        reqId: cozeData.chat_id,
        encrypted: false,
        runningLog: '',
        query: '',
        messages: '',
        packageType: 'data',
        errorMsg: '',
      };
    }

    // conversation.chat.completed事件 - 会话完成
    if (event === 'conversation.chat.completed') {
      const result = {
        status: 'success',
        response: '',
        responseAll: this.accumulatedContent,
        finished: true,
        useTimes: cozeData.time_cost?.total_duration_ms || 0,
        useTokens: 0,
        resultMap: {
          steps: [],
        },
        responseType: 'text',
        voiceUrl: '',
        traceId: cozeData.conversation_id,
        reqId: cozeData.chat_id,
        encrypted: false,
        runningLog: '',
        query: '',
        messages: '',
        packageType: 'data',
        errorMsg: '',
      };
      // 重置累积内容
      this.accumulatedContent = '';
      return result;
    }

    // done事件
    if (event === 'done') {
      const result = {
        status: 'success',
        response: '',
        responseAll: this.accumulatedContent,
        finished: true,
        useTimes: 0,
        useTokens: 0,
        resultMap: {
          steps: [],
        },
        responseType: 'text',
        voiceUrl: '',
        traceId: '',
        reqId: '',
        encrypted: false,
        runningLog: '',
        query: '',
        messages: '',
        packageType: 'data',
        errorMsg: '',
      };
      // 重置累积内容
      this.accumulatedContent = '';
      return result;
    }

    // 其他类型的消息（follow_up、verbose等）暂时忽略，返回心跳包
    return {
      status: 'success',
      response: '',
      responseAll: this.accumulatedContent,
      finished: false,
      useTimes: 0,
      useTokens: 0,
      resultMap: {
        steps: [],
      },
      responseType: 'text',
      voiceUrl: '',
      traceId: cozeData.conversation_id || '',
      reqId: cozeData.chat_id || '',
      encrypted: false,
      runningLog: '',
      query: '',
      messages: '',
      packageType: 'heartbeat', // 设置为心跳包，不会触发UI更新
      errorMsg: '',
    };
  }

  /**
   * 重置转换器状态
   */
  reset() {
    this.accumulatedContent = '';
  }
}

/**
 * 通义千问智能体转换器
 * 将通义千问的流式输出转换为本地MESSAGE.Answer格式
 */
class TongyiConverter implements AgentConverter<TongyiStreamData> {
  /**
   * 转换通义千问流式输出为MESSAGE.Answer格式
   */
  convert(tongyiData: TongyiStreamData, event: string): MESSAGE.Answer {
    // message 事件 - 通义千问直接返回完整消息
    if (event === 'message') {
      const choice = tongyiData.response?.choices?.[0];
      const content = choice?.message?.content || '';
      const isFinished = choice?.finishReason === 'stop';

      return {
        status: 'success',
        response: content,
        responseAll: content,
        finished: isFinished,
        useTimes: 0,
        useTokens: tongyiData.outputTokens || 0,
        resultMap: {
          steps: [],
          eventData: {
            messageOrder: 0,
            messageType: 'task',
            resultMap: {
              messageTime: new Date().toISOString(),
              messageType: 'result',
              result: content,
              isFinal: isFinished,
              requestId: tongyiData.response?.id,
              messageId: tongyiData.response?.id,
              finish: isFinished,
              id: tongyiData.response?.id,
              resultMap: {
                steps: [],
                taskSummary: content,
              },
            },
            messageId: tongyiData.response?.id,
            taskId: tongyiData.threadId,
            taskOrder: 0,
          },
        },
        responseType: 'text',
        voiceUrl: '',
        traceId: tongyiData.traceId,
        reqId: tongyiData.response?.id,
        encrypted: false,
        runningLog: '',
        query: '',
        messages: '',
        packageType: 'data',
        errorMsg: '',
      };
    }

    // done 事件
    if (event === 'done') {
      return {
        status: 'success',
        response: '',
        responseAll: '',
        finished: true,
        useTimes: 0,
        useTokens: 0,
        resultMap: {
          steps: [],
        },
        responseType: 'text',
        voiceUrl: '',
        traceId: '',
        reqId: '',
        encrypted: false,
        runningLog: '',
        query: '',
        messages: '',
        packageType: 'heartbeat',
        errorMsg: '',
      };
    }

    // 其他类型返回心跳包
    return {
      status: 'success',
      response: '',
      responseAll: '',
      finished: false,
      useTimes: 0,
      useTokens: 0,
      resultMap: {
        steps: [],
      },
      responseType: 'text',
      voiceUrl: '',
      traceId: tongyiData.traceId || '',
      reqId: tongyiData.response?.id || '',
      encrypted: false,
      runningLog: '',
      query: '',
      messages: '',
      packageType: 'heartbeat',
      errorMsg: '',
    };
  }

  reset() {
    // 通义千问不需要累积状态，每次都是完整消息
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

