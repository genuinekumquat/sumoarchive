package com.torikumilab.sumoarchive.controller;

import com.torikumilab.sumoarchive.service.HeyaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Optional;

/**
 * 헤야 페이지 - 메인 "일문" 탭의 헤야 이름, 리키시 프로필의 소속 헤야에서 들어온다.
 */
@Controller
@RequiredArgsConstructor
public class HeyaViewController {

	private final HeyaService heyaService;

	@GetMapping("/heya/{id}")
	public ModelAndView detail(@PathVariable Integer id) {
		// 다른 헤야로 합쳐진(개명·흡수) 헤야의 옛 주소는 새 헤야 페이지로 영구 이동 (lang 등 쿼리는 유지)
		Optional<Integer> target = heyaService.mergedTarget(id);
		if (target.isPresent()) {
			RedirectView redirect = new RedirectView("/heya/" + target.get(), true);
			redirect.setPropagateQueryParams(true);
			redirect.setExposeModelAttributes(false); // SeoModelAdvice 값(baseUrl 등)이 쿼리로 붙지 않게
			redirect.setStatusCode(HttpStatus.MOVED_PERMANENTLY);
			return new ModelAndView(redirect);
		}
		return new ModelAndView("heya/detail", "heya", heyaService.getHeyaPage(id));
	}
}
