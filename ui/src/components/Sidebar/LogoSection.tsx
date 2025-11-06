import { memo } from 'react';

/**
 * Logo区域组件
 * 负责显示应用Logo和标题
 */
const LogoSection: GenieType.FC = memo(() => {
  return (
    <div className="mb-6 text-center">
      {/* Logo图标 */}
      <div className="inline-flex items-center justify-center w-16 h-16 rounded-3xl bg-gradient-to-br from-[#4040ff] to-[#764ba2] shadow-xl shadow-[#4040ff]/30 mb-4">
        <svg className="w-9 h-9 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z" />
        </svg>
      </div>

      {/* 应用标题 */}
      <h1 className="text-3xl font-black bg-gradient-to-r from-[#4040ff] via-[#5050ff] to-[#764ba2] bg-clip-text text-transparent tracking-tight leading-tight">
        JD Genie
      </h1>

      {/* 副标题 */}
      <p className="text-sm text-gray-600 mt-1.5 font-semibold">智能问答助手</p>
    </div>
  );
});

LogoSection.displayName = 'LogoSection';

export default LogoSection;